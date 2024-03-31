package io.rewynd.android.player

import android.app.Notification
import android.app.Notification.MediaStyle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C.TIME_UNSET
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.util.EventLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.android.R
import io.rewynd.android.browser.BrowserActivity
import io.rewynd.android.browser.BrowserState
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.client.cookie.CookieStorageCookieJar
import io.rewynd.android.client.cookie.PersistentCookiesStorage
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.android.login.MainActivity
import io.rewynd.android.model.PlayerMedia
import io.rewynd.android.player.StreamHeartbeat.Companion.copy
import io.rewynd.client.RewyndClient
import io.rewynd.model.Progress
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class PlayerService : Service() {
    private val playbackStateBuilder: PlaybackState.Builder = PlaybackState.Builder()
    private var originalPlayerProps: PlayerProps? = null
    private val next: MutableStateFlow<PlayerMedia?> = MutableStateFlow(null)
    private val prev: MutableStateFlow<PlayerMedia?> = MutableStateFlow(null)
    private val bufferedPosition: MutableStateFlow<Duration> = MutableStateFlow(0.milliseconds)
    private val currentPlayerTime: MutableStateFlow<Duration> = MutableStateFlow(0.milliseconds)
    private val media: MutableStateFlow<PlayerMedia?> = MutableStateFlow(null)
    private val isLoading: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private val cookies by lazy { PersistentCookiesStorage(this.applicationContext) }
    private val mutex by lazy { Mutex() }
    private lateinit var notification: Notification
    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build().apply {
            addListener(listener)
            addAnalyticsListener(EventLogger())
            playWhenReady = true
        }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(1.minutes.toJavaDuration())
            .cookieJar(CookieStorageCookieJar(cookies))
            .addInterceptor(logging)
            .build()
    }
    private val datasourceFactory by lazy { OkHttpDataSource.Factory(httpClient) }
    private val mediaSession: MediaSession by lazy { MediaSession(this, "RewyndMediaSession") }

    private lateinit var client: RewyndClient

    @OptIn(UnstableApi::class) // HlsMediaSource is unstable
    private val heartbeat by lazy {
        StreamHeartbeat(
            client = client,
            onCanceled = {
                log.info { "Heartbeat Cancelled" }
                it.copy(startOffset = currentTime.inWholeMilliseconds / 1000.0)
            },
            onAvailable = {
                log.info { "Heartbeat Available" }
                putProgress()
            },
        ) {
            log.info { "Heartbeat Loading" }

            val uri = client.baseUrl + it.url
            Log.i("MEDIA URI", uri)

            val source =
                HlsMediaSource.Factory(datasourceFactory)
                    .setLoadErrorHandlingPolicy(
                        object : LoadErrorHandlingPolicy {
                            override fun getFallbackSelectionFor(
                                p0: LoadErrorHandlingPolicy.FallbackOptions,
                                p1: LoadErrorHandlingPolicy.LoadErrorInfo,
                            ): LoadErrorHandlingPolicy.FallbackSelection? = null

                            override fun getRetryDelayMsFor(p0: LoadErrorHandlingPolicy.LoadErrorInfo): Long = 1000L

                            override fun getMinimumLoadableRetryCount(p0: Int): Int = Int.MAX_VALUE
                        },
                    )
                    .createMediaSource(
                        MediaItem.fromUri(
                            uri,
                        ).buildUpon()
                            .setLiveConfiguration(
                                MediaItem.LiveConfiguration.Builder()
                                    .setMaxOffsetMs(Long.MAX_VALUE)
                                    .setMinOffsetMs(0)
                                    .setTargetOffsetMs(TIME_UNSET)
                                    .build(),
                            )
                            .build(),
                    )

            player.setMediaSource(source)
            player.prepare()
            this.isLoading.value = false
        }
    }

    private fun stop() =
        runBlocking {
            this@PlayerService.stopSelf()
            this@PlayerService.player.stop()
            this@PlayerService.heartbeat.unload()
            this@PlayerService.destroyNotification()
        }

    private fun pause() {
        player.pause()
        createNotification()
    }

    private fun play() {
        player.playWhenReady = true
        player.play()
        createNotification()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private val isPlayingState by lazy { MutableStateFlow(player.playWhenReady) }
    private val listener =
        object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                log.info { "Got player events" }
                super.onEvents(player, events)
                currentPlayerTime.value = player.currentPosition.milliseconds
                bufferedPosition.value = player.bufferedPosition.milliseconds
                createNotification()
            }

            override fun onPlayerError(e: PlaybackException) =
                runBlocking {
                    log.error(e) { "Player Error!" }
                    media.value?.copy(player.currentPosition.milliseconds)
                        ?.let { this@PlayerService.internalLoadMedia(it) } ?: Unit
                }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                runBlocking {
                    isPlayingState.emit(isPlaying)
                    this@PlayerService.setPlaybackState()
                    createNotification()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) =
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        runBlocking {
                            log.info { "Got state ended" }
                            next.value?.let {
                                this@PlayerService.internalLoadMedia(it)
                            }
                        }
                        Unit
                    }

                    Player.STATE_BUFFERING -> {
                        log.info { "Got Buffering state" }
                    }

                    Player.STATE_IDLE -> {
                        log.info { "Got Idle state" }
                    }

                    Player.STATE_READY -> {
                        log.info { "Got Ready state" }
                    }

                    else -> {
                        log.warn { "Got unknown state: $playbackState" }
                    }
                }
        }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        intent ?: return START_NOT_STICKY
        val propStr =
            requireNotNull(intent.extras?.getString(PLAYER_SERVICE_INTENT_BUNDLE_PROPS_KEY)) {
                "$PLAYER_SERVICE_INTENT_BUNDLE_PROPS_KEY not included in intent triggering PlayerService"
            }

        Log.i("PlayerService", propStr)
        when (val props: PlayerServiceProps = Json.decodeFromString(propStr)) {
            is PlayerServiceProps.Start -> {
                handleStartIntent(props)
            }

            is PlayerServiceProps.Pause -> {
                pause()
            }

            is PlayerServiceProps.Play -> {
                play()
            }

            is PlayerServiceProps.Stop -> {
                stop()
                startActivity(Intent(this@PlayerService, BrowserActivity::class.java))
                return START_NOT_STICKY
            }

            is PlayerServiceProps.Next -> {
                val n = next.value
                if (n != null) {
                    runBlocking {
                        internalLoadMedia(n)
                    }
                }
            }

            is PlayerServiceProps.Prev -> {
                val p = prev.value
                if (p != null) {
                    runBlocking {
                        internalLoadMedia(p)
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun handleStartIntent(props: PlayerServiceProps.Start) {
        this.originalPlayerProps = props.playerProps
        this.media.value = props.playerProps.media
        client = mkRewyndClient(props.serverUrl)
        if (props.interruptPlayback || this.media.value == null) {
            runBlocking {
                internalLoadMedia(props.playerProps.media)
            }
        }
        this.mediaSession.setCallback(mediaSessionCallback)
        this.setPlaybackState()
        _instance.value = serviceInterface
    }

    private fun setPlaybackState() {
        val playPause =
            if (this.isPlayingState.value) {
                PlaybackState.ACTION_PAUSE
            } else {
                PlaybackState.ACTION_PLAY
            }

        val next =
            if (this.next.value == null) {
                0
            } else {
                PlaybackState.ACTION_SKIP_TO_NEXT
            }

        val prev =
            if (this.prev.value == null) {
                0
            } else {
                PlaybackState.ACTION_SKIP_TO_PREVIOUS
            }

        this.playbackStateBuilder.setActions(
            playPause or next or prev or PlaybackState.ACTION_FAST_FORWARD or PlaybackState.ACTION_REWIND or PlaybackState.ACTION_STOP,
        ).setState(
            when (this.player.playbackState) {
                Player.STATE_BUFFERING -> PlaybackState.STATE_BUFFERING
                Player.STATE_IDLE -> PlaybackState.STATE_PAUSED
                Player.STATE_READY -> if (this.player.playWhenReady) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
                Player.STATE_ENDED -> PlaybackState.STATE_PAUSED
                else -> PlaybackState.STATE_NONE
            },
            this.currentTime.inWholeMilliseconds,
            1.0f,
        )

        this.mediaSession.isActive = true

        this.mediaSession.setPlaybackState(this.playbackStateBuilder.build())
    }

    private fun destroyNotification() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(PLAYER_SERVICE_NOTIFICATION_CHANNEL_ID)
        }
    }

    private fun mkPlayerServiceIntent(props: PlayerServiceProps): PendingIntent =
        Intent(
            this,
            PlayerService::class.java,
        ).apply {
            putExtra(
                PLAYER_SERVICE_INTENT_BUNDLE_PROPS_KEY,
                Json.encodeToString(props),
            )
        }.let {
            PendingIntent.getService(
                this,
                props.requestCode,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

    private fun mkMediaIntent(playerMedia: PlayerMedia) =
        Intent(
            this,
            PlayerActivity::class.java,
        ).apply {
            putExtra(
                PlayerActivity.PLAYER_ACTIVITY_PROPS_EXTRA_NAME,
                Json.encodeToString(
                    PlayerActivityProps(
                        PlayerProps(playerMedia, originalPlayerProps?.browserState ?: emptyList()),
                        ServerUrl(client.baseUrl),
                        false,
                    ),
                ),
            )
        }.let { notificationIntent ->
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT,
            )
        }

    private fun mkMainIntent() =
        Intent(
            this,
            MainActivity::class.java,
        ).apply {
        }.let { notificationIntent ->
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT,
            )
        }

    private fun createNotificationChannel() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = getString(R.string.notification_title)
            val descriptionText = getString(R.string.notification_title)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel =
                NotificationChannel(PLAYER_SERVICE_NOTIFICATION_CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        } else {
            Unit
        }

    // https://android-developers.googleblog.com/2020/08/playing-nicely-with-media-controls.html
    private fun createNotification() =
        this.media.value?.let { playerMedia ->
            // Creating a NotificationChannel is required for Android O and up.
            createNotificationChannel()

            val playerPendingIntent = mkMediaIntent(playerMedia)
            val stopPendingIntent = mkPlayerServiceIntent(PlayerServiceProps.Stop)
            val nextPendingIntent = mkPlayerServiceIntent(PlayerServiceProps.Next)
            val prevPendingIntent = mkPlayerServiceIntent(PlayerServiceProps.Prev)
            val pausePendingIntent = mkPlayerServiceIntent(PlayerServiceProps.Pause)
            val playPendingIntent = mkPlayerServiceIntent(PlayerServiceProps.Play)

            notification =
                (
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Notification.Builder(
                            this,
                            PLAYER_SERVICE_NOTIFICATION_CHANNEL_ID,
                        )
                    } else {
                        Notification.Builder(this)
                    }
                ).apply {
                    setProgress(
                        playerMedia.runTime.inWholeSeconds.toInt(),
                        currentTime.inWholeSeconds.toInt(),
                        false,
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setFlag(Notification.FLAG_ONGOING_EVENT and Notification.FLAG_NO_CLEAR, true)
                    }
                    setOngoing(true)
                    setContentTitle(playerMedia.title)
                    setSmallIcon(R.drawable.rewynd_icon_basic_monochrome)
                    // Enable launching the player by clicking the notification
                    setContentIntent(playerPendingIntent)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setAllowSystemGeneratedContextualActions(false)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setColorized(true)
                    }

                    // TODO use non-private icons
                    // 0
                    addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource(
                                this@PlayerService,
                                androidx.media3.ui.R.drawable.exo_icon_stop,
                            ),
                            "Stop",
                            stopPendingIntent,
                        ).build(),
                    )
                    // 1
                    addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource(
                                this@PlayerService,
                                androidx.media3.ui.R.drawable.exo_icon_previous,
                            ),
                            "Previous",
                            prevPendingIntent,
                        ).build(),
                    )
                    // 2
                    addAction(
                        if (isPlayingState.value) {
                            Notification.Action.Builder(
                                Icon.createWithResource(
                                    this@PlayerService,
                                    androidx.media3.ui.R.drawable.exo_icon_pause,
                                ),
                                "Pause",
                                pausePendingIntent,
                            ).build()
                        } else {
                            Notification.Action.Builder(
                                Icon.createWithResource(
                                    this@PlayerService,
                                    androidx.media3.ui.R.drawable.exo_icon_play,
                                ),
                                "Play",
                                playPendingIntent,
                            ).build()
                        },
                    )
                    // 3
                    addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource(
                                this@PlayerService,
                                androidx.media3.ui.R.drawable.exo_icon_next,
                            ),
                            "Next",
                            nextPendingIntent,
                        ).build(),
                    )
                    setColor(Color.GREEN)
                    style =
                        MediaStyle()
                            .setMediaSession(mediaSession.sessionToken)
                            .setShowActionsInCompactView(1, 2, 3)
                    setDeleteIntent(stopPendingIntent)
                }.build()

            startForeground(PLAYER_SERVICE_NOTIFICATION_ID, notification)
        } ?: Unit

    private val currentTime: Duration
        get() =
            (
                player.currentPosition + (
                    this.media.value?.startOffset?.inWholeMilliseconds
                        ?: 0
                )
            ).milliseconds

    suspend fun internalLoadMedia(media: PlayerMedia) =
        mutex.withLock {
            this.isLoading.value = true
            this.media.value = media
            this.next.value = PlaybackMethodHandler.next(client, media)
            this.prev.value = PlaybackMethodHandler.prev(client, media)
            heartbeat.load(media.toCreateStreamRequest())
            this.setPlaybackState()
            createNotification()
        }

    private fun putProgress() =
        MainScope().launch {
            when (val m = media.value) {
                null -> {}
                is PlayerMedia.Episode -> {
                    kotlin.runCatching {
                        client.putUserProgress(
                            Progress(
                                m.info.id,
                                (currentTime.inWholeMilliseconds / 1000.0) / m.info.runTime,
                                Clock.System.now(),
                            ),
                        )
                    }.onFailure { log.error(it) { "Failed to putUserProgress" } }
                }
            }
        }

    private fun seek(desired: Duration) =
        this.media.value?.let { playerMedia ->
            if (desired > playerMedia.startOffset && desired < playerMedia.startOffset + this.player.duration.milliseconds) {
                this.player.seekTo((desired - playerMedia.startOffset).inWholeMilliseconds)
            } else {
                runBlocking {
                    this@PlayerService.internalLoadMedia(playerMedia.copy(startOffset = desired))
                }
            }
        } ?: Unit

    companion object {
        private val log by lazy { KotlinLogging.logger { } }
        const val PLAYER_SERVICE_INTENT_BUNDLE_PROPS_KEY = "PlayerServiceProps"
        const val PLAYER_SERVICE_NOTIFICATION_ID = 1
        const val PLAYER_SERVICE_NOTIFICATION_CHANNEL_ID = "RewyndServiceNotification"
        private val logging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

        private val _instance: MutableStateFlow<PlayerServiceInterface?> = MutableStateFlow(null)
        val instance: StateFlow<PlayerServiceInterface?>
            get() = _instance
    }

    private val serviceInterface =
        object : PlayerServiceInterface {
            override val browserState: List<BrowserState>
                get() = this@PlayerService.originalPlayerProps?.browserState ?: emptyList()
            override val player: ExoPlayer
                get() = this@PlayerService.player
            override val isLoading: StateFlow<Boolean>
                get() = this@PlayerService.isLoading
            override val isPlayingState: StateFlow<Boolean>
                get() = this@PlayerService.isPlayingState
            override val media: StateFlow<PlayerMedia?>
                get() = this@PlayerService.media
            override val next: StateFlow<PlayerMedia?>
                get() = this@PlayerService.next
            override val prev: StateFlow<PlayerMedia?>
                get() = this@PlayerService.prev
            override val bufferedPosition: StateFlow<Duration>
                get() = this@PlayerService.bufferedPosition
            override val currentPlayerTime: StateFlow<Duration>
                get() = this@PlayerService.currentPlayerTime
            override val nextPendingIntent: PendingIntent?
                get() =
                    next.value?.let {
                        this@PlayerService.mkPlayerServiceIntent(
                            PlayerServiceProps.Next,
                        )
                    }
            override val prevPendingIntent: PendingIntent?
                get() =
                    prev.value?.let {
                        this@PlayerService.mkPlayerServiceIntent(
                            PlayerServiceProps.Prev,
                        )
                    }
            override val pausePendingIntent: PendingIntent?
                get() =
                    if (isPlayingState.value) {
                        this@PlayerService.mkPlayerServiceIntent(
                            PlayerServiceProps.Pause,
                        )
                    } else {
                        null
                    }
            override val playPendingIntent: PendingIntent?
                get() =
                    if (isPlayingState.value) {
                        null
                    } else {
                        this@PlayerService.mkPlayerServiceIntent(
                            PlayerServiceProps.Play,
                        )
                    }

            override fun playNext(): Unit =
                this@PlayerService.next.value?.let {
                    MainScope().launch {
                        this@PlayerService.internalLoadMedia(it)
                    }
                    Unit
                } ?: Unit

            override fun playPrev() =
                this@PlayerService.prev.value?.let {
                    MainScope().launch {
                        this@PlayerService.internalLoadMedia(it)
                    }
                    Unit
                } ?: Unit

            override fun stop() = this@PlayerService.stop()

            override fun pause() = this@PlayerService.pause()

            override fun play() = this@PlayerService.play()

            override fun seek(desired: Duration) = this@PlayerService.seek(desired)

            override suspend fun loadMedia(mediaInfo: PlayerMedia) = this@PlayerService.internalLoadMedia(mediaInfo)
        }

    private val mediaSessionCallback =
        object : MediaSession.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                // TODO make user able to disable media buttons via a preference.
                // Disabling media buttons can be done by not calling the super method here, and returning true instead.
                return super.onMediaButtonEvent(mediaButtonIntent)
            }

            override fun onStop() = this@PlayerService.stop()

            override fun onPlay() = this@PlayerService.play()

            override fun onPause() = this@PlayerService.pause()

            override fun onFastForward() = this@PlayerService.player.seekForward()

            override fun onRewind() = this@PlayerService.player.seekBack()

            override fun onSkipToNext() =
                runBlocking {
                    val next = this@PlayerService.next.value
                    if (next != null) {
                        this@PlayerService.internalLoadMedia(next)
                    }
                }

            override fun onSkipToPrevious() =
                runBlocking {
                    val prev = this@PlayerService.prev.value
                    if (prev != null) {
                        this@PlayerService.internalLoadMedia(prev)
                    }
                }
        }
}
