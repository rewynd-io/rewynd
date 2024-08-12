package io.rewynd.android.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import io.rewynd.android.R
import io.rewynd.android.browser.BrowserActivity
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.client.cookie.CookieStorageCookieJar
import io.rewynd.android.client.cookie.PersistentCookiesStorage
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.android.model.PlayerMedia
import io.rewynd.android.player.StreamHeartbeat.Companion.copy
import io.rewynd.client.RewyndClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class PlayerService : Service() {
    private var browserState: Bundle? = null
    private var originalPlayerProps: PlayerProps? = null
    private val next: MutableStateFlow<PlayerMedia?> = MutableStateFlow(null)
    private val prev: MutableStateFlow<PlayerMedia?> = MutableStateFlow(null)
    private val cookies by lazy { PersistentCookiesStorage(this.applicationContext) }
    private lateinit var notification: Notification
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .callTimeout(1.minutes.toJavaDuration())
            .cookieJar(CookieStorageCookieJar(cookies))
            .addInterceptor(logging)
            .build()
    }

    private suspend fun onNext(playerWrapper: PlayerWrapper) {
        next.value =
            next.value?.let { nonNullNext ->
                prev.value = playerWrapper.media.value
                playerWrapper.load(nonNullNext)
                PlaybackMethodHandler.next(client, nonNullNext)
            }
    }

    private suspend fun onPrev(playerWrapper: PlayerWrapper) {
        prev.value =
            prev.value?.let { nonNullPrev ->
                next.value = playerWrapper.media.value
                playerWrapper.load(nonNullPrev)
                PlaybackMethodHandler.prev(client, nonNullPrev)
            }
    }

    private val player by lazy {
        PlayerWrapper(this, httpClient, client, onEvent = {
            // Relies on the stop() setting instance.value to null, which isn't great
            if (_instance.value != null) {
                setPlaybackState()
                createNotification()
            }
        }, onNext = { playerWrapper ->
            next.value =
                next.value?.let { nonNullNext ->
                    prev.value = playerWrapper.media.value
                    playerWrapper.load(nonNullNext.atBeginning())
                    PlaybackMethodHandler.next(client, nonNullNext)
                }
        })
    }

    private val mediaSession: MediaSession by lazy { MediaSession(this, "RewyndMediaSession") }

    private lateinit var client: RewyndClient

    private fun stop(): Unit =
        runBlocking {
            _instance.value = null // Tells the playerWrapper not to recreate the notification, amongst other things
            this@PlayerService.player.stop()
            this@PlayerService.destroyNotification()
            val intent = Intent(this@PlayerService, PlayerActivity::class.java)
                .putExtra(
                    PlayerActivity.PLAYER_ACTIVITY_ACTION_KEY,
                    Json.encodeToString<PlayerActivityAction>(PlayerActivityAction.Stop),
                ).setFlags(FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

    override fun onBind(p0: Intent?): IBinder? = null

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

        this.browserState = intent.getBundleExtra(PLAYER_SERVICE_INTENT_BUNDLE_BROWSER_STATE_KEY) ?: this.browserState
        Log.i("SERVICE_BROWSER_STATE", this.browserState.toString())
        Log.i("PlayerService", propStr)
        when (val props: PlayerServiceProps = Json.decodeFromString(propStr)) {
            is PlayerServiceProps.Start -> {
                handleStartIntent(props)
            }

            is PlayerServiceProps.Pause -> {
                player.pause()
            }

            is PlayerServiceProps.Play -> {
                player.play()
            }

            is PlayerServiceProps.Stop -> {
                stop()
                startActivity(
                    Intent(
                        this@PlayerService,
                        BrowserActivity::class.java,
                    ).apply {
                        putExtra(
                            BrowserActivity.BROWSER_STATE,
                            browserState,
                        )
                        addFlags(FLAG_ACTIVITY_NEW_TASK)
                    },
                )
                return START_NOT_STICKY
            }

            is PlayerServiceProps.Next -> {
                val n = next.value
                if (n != null) {
                    runBlocking {
                        player.load(n)
                    }
                }
            }

            is PlayerServiceProps.Prev -> {
                val p = prev.value
                if (p != null) {
                    runBlocking {
                        player.load(p)
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun handleStartIntent(props: PlayerServiceProps.Start) {
        if (props.interruptPlayback || player.media.value == null) {
            this.originalPlayerProps = props.playerProps
            client = mkRewyndClient(props.serverUrl)
            runBlocking {
                val playerMedia = props.playerProps.media
                this@PlayerService.next.value = PlaybackMethodHandler.next(client, playerMedia)
                this@PlayerService.prev.value = PlaybackMethodHandler.prev(client, playerMedia)
                player.load(playerMedia)
            }
            this.mediaSession.setCallback(mediaSessionCallback)
            this.setPlaybackState()
            _instance.value = serviceInterface
        }
    }

    private fun setPlaybackState() {
        val playPause =
            if (this.player.isPlayingState.value) {
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

        val playbackStateBuilder = PlaybackState.Builder()
            .setActions(
                playPause or
                    next or
                    prev or
                    PlaybackState.ACTION_FAST_FORWARD or
                    PlaybackState.ACTION_REWIND or
                    PlaybackState.ACTION_STOP,
            ).setState(
                when (this.player.playbackState.value) {
                    Player.STATE_BUFFERING -> PlaybackState.STATE_BUFFERING
                    Player.STATE_IDLE -> PlaybackState.STATE_PAUSED
                    Player.STATE_READY ->
                        if (this.player.isPlayingState.value) {
                            PlaybackState.STATE_PLAYING
                        } else {
                            PlaybackState.STATE_PAUSED
                        }

                    Player.STATE_ENDED -> PlaybackState.STATE_PAUSED
                    else -> PlaybackState.STATE_NONE
                },
                this.player.currentOffsetTime.inWholeMilliseconds,
                1.0f,
            ).setBufferedPosition(player.bufferedPosition.value.inWholeMilliseconds)
            .addCustomAction(
                PlaybackState.CustomAction.Builder(
                    CUSTOM_ACTION_STOP,
                    "Stop",
                    androidx.media3.ui.R.drawable.exo_icon_stop
                )
                    .build()
            )
            .build()

        this.mediaSession.isActive = true
        this.mediaSession.setPlaybackState(playbackStateBuilder)
        this.mediaSession.setMetadata(
            MediaMetadata
                .Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, player.media.value?.title)
                // Artist.
                // Could also be the channel name or TV series.
                .putString(MediaMetadata.METADATA_KEY_ARTIST, player.media.value?.artist)
                // TODO load media image as a bitmap
                // Album art.
                // Could also be a screenshot or hero image for video content
                // The URI scheme needs to be "content", "file", or "android.resource".
//            .putBitmap(
//                MediaMetadata.METADATA_KEY_ALBUM_ART_URI, currentTrack.albumArtUri)
                // Duration.
                // If duration isn't set, such as for live broadcasts, then the progress
                // indicator won't be shown on the seekbar.
                .putLong(
                    MediaMetadata.METADATA_KEY_DURATION,
                    player.media.value
                        ?.runTime
                        ?.inWholeMilliseconds ?: 0,
                ) // 4
                .build(),
        )
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
                PlayerActivity.PLAYER_ACTIVITY_ACTION_KEY,
                Json.encodeToString<PlayerActivityAction>(
                    PlayerActivityAction.Start(
                        PlayerActivityProps(
                            PlayerProps(playerMedia),
                            ServerUrl(client.baseUrl),
                            false,
                        ),
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
    @Suppress("MagicNumber")
    private fun createNotification(): Unit =
        this.player.media.value?.let { playerMedia ->
            // Creating a NotificationChannel is required for Android O and up.
            createNotificationChannel()

            val playerPendingIntent = mkMediaIntent(playerMedia)
            val stopPendingIntent = mkPlayerServiceIntent(PlayerServiceProps.Stop)
            val nextPendingIntent = mkPlayerServiceIntent(PlayerServiceProps.Next)
            val prevPendingIntent = mkPlayerServiceIntent(PlayerServiceProps.Prev)
            val pausePendingIntent = mkPlayerServiceIntent(PlayerServiceProps.Pause)
            val playPendingIntent = mkPlayerServiceIntent(PlayerServiceProps.Play)

            val builder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Notification.Builder(
                        this,
                        PLAYER_SERVICE_NOTIFICATION_CHANNEL_ID,
                    )
                } else {
                    Notification.Builder(this)
                }

            mediaSession.setSessionActivity(playerPendingIntent)
            val style =
                Notification.MediaStyle().apply {
                    setMediaSession(mediaSession.sessionToken)
                    setShowActionsInCompactView(1, 2, 3)
                }

            builder
                .setStyle(style)
                .setSmallIcon(R.drawable.rewynd_icon_basic_monochrome)
                .setColor(Color.GREEN)
                .setContentIntent(playerPendingIntent)
                .setProgress(
                    playerMedia.runTime.inWholeSeconds.toInt(),
                    player.currentOffsetTime.inWholeSeconds.toInt(),
                    false,
                ).addAction(
                    Notification.Action
                        .Builder(
                            Icon.createWithResource(
                                this@PlayerService,
                                androidx.media3.ui.R.drawable.exo_icon_stop,
                            ),
                            "Stop",
                            stopPendingIntent,
                        ).build(),
                ).addAction(
                    Notification.Action
                        .Builder(
                            Icon.createWithResource(
                                this@PlayerService,
                                androidx.media3.ui.R.drawable.exo_icon_previous,
                            ),
                            "Previous",
                            prevPendingIntent,
                        ).build(),
                ).addAction(
                    if (this@PlayerService.player.isPlayingState.value) {
                        Notification.Action
                            .Builder(
                                Icon.createWithResource(
                                    this@PlayerService,
                                    androidx.media3.ui.R.drawable.exo_icon_pause,
                                ),
                                "Pause",
                                pausePendingIntent,
                            ).build()
                    } else {
                        Notification.Action
                            .Builder(
                                Icon.createWithResource(
                                    this@PlayerService,
                                    androidx.media3.ui.R.drawable.exo_icon_play,
                                ),
                                "Play",
                                playPendingIntent,
                            ).build()
                    },
                ).addAction(
                    Notification.Action
                        .Builder(
                            Icon.createWithResource(
                                this@PlayerService,
                                androidx.media3.ui.R.drawable.exo_icon_next,
                            ),
                            "Next",
                            nextPendingIntent,
                        ).build(),
                )
                .setDeleteIntent(stopPendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setColorized(true)
            }

            notification = builder.build()
//
// //            notification =
//            (
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        Notification.Builder(
//                            this,
//                            PLAYER_SERVICE_NOTIFICATION_CHANNEL_ID,
//                        )
//                    } else {
//                        Notification.Builder(this)
//                    }
//                    ).apply {
//                    setProgress(
//                        playerMedia.runTime.inWholeSeconds.toInt(),
//                        player.currentOffsetTime.inWholeSeconds.toInt(),
//                        false,
//                    )
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                        setFlag(Notification.FLAG_ONGOING_EVENT and Notification.FLAG_NO_CLEAR, true)
//                    }
//                    setOngoing(true)
//                    setContentTitle(playerMedia.title)
//                    setSmallIcon(R.drawable.rewynd_icon_basic_monochrome)
//                    // Enable launching the player by clicking the notification
//                    setContentIntent(playerPendingIntent)
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                        setAllowSystemGeneratedContextualActions(false)
//                    }
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        setColorized(true)
//                    }
//
//                    // TODO use non-private icons
//                    // 0
//                    addAction(
//                        Notification.Action.Builder(
//                            Icon.createWithResource(
//                                this@PlayerService,
//                                androidx.media3.ui.R.drawable.exo_icon_stop,
//                            ),
//                            "Stop",
//                            stopPendingIntent,
//                        ).build(),
//                    )
//                    // 1
//                    addAction(
//                        Notification.Action.Builder(
//                            Icon.createWithResource(
//                                this@PlayerService,
//                                androidx.media3.ui.R.drawable.exo_icon_previous,
//                            ),
//                            "Previous",
//                            prevPendingIntent,
//                        ).build(),
//                    )
//                    // 2
//                    addAction(
//                        if (this@PlayerService.player.isPlayingState.value) {
//                            Notification.Action.Builder(
//                                Icon.createWithResource(
//                                    this@PlayerService,
//                                    androidx.media3.ui.R.drawable.exo_icon_pause,
//                                ),
//                                "Pause",
//                                pausePendingIntent,
//                            ).build()
//                        } else {
//                            Notification.Action.Builder(
//                                Icon.createWithResource(
//                                    this@PlayerService,
//                                    androidx.media3.ui.R.drawable.exo_icon_play,
//                                ),
//                                "Play",
//                                playPendingIntent,
//                            ).build()
//                        },
//                    )
//                    // 3
//                    addAction(
//                        Notification.Action.Builder(
//                            Icon.createWithResource(
//                                this@PlayerService,
//                                androidx.media3.ui.R.drawable.exo_icon_next,
//                            ),
//                            "Next",
//                            nextPendingIntent,
//                        ).build(),
//                    )
//                    setColor(Color.GREEN)
// //                    style =
// //                        Notification.MediaStyle()
// //                            .setMediaSession(mediaSession.sessionToken)
// //                            .setShowActionsInCompactView(1, 2, 3)
//                    setDeleteIntent(stopPendingIntent)
//                }.build()

            startForeground(PLAYER_SERVICE_NOTIFICATION_ID, notification)
        } ?: Unit

    companion object {
        const val CUSTOM_ACTION_STOP = "Stop"
        const val PLAYER_SERVICE_INTENT_BUNDLE_PROPS_KEY = "PlayerServiceProps"
        const val PLAYER_SERVICE_INTENT_BUNDLE_BROWSER_STATE_KEY = "PlayerServiceBrowserState"
        const val PLAYER_SERVICE_NOTIFICATION_ID = 1
        const val PLAYER_SERVICE_NOTIFICATION_CHANNEL_ID = "RewyndServiceNotification"
        private val logging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

        private val _instance: MutableStateFlow<PlayerServiceInterface?> = MutableStateFlow(null)
        val instance: StateFlow<PlayerServiceInterface?>
            get() = _instance
    }

    private val serviceInterface =
        object : PlayerServiceInterface {
            override val browserState: Bundle?
                get() = this@PlayerService.browserState
            override val isLoading: StateFlow<Boolean>
                get() = this@PlayerService.player.isLoading
            override val isPlayingState: StateFlow<Boolean>
                get() = this@PlayerService.player.isPlayingState
            override val media: StateFlow<PlayerMedia?>
                get() = this@PlayerService.player.media
            override val next: StateFlow<PlayerMedia?>
                get() = this@PlayerService.next
            override val prev: StateFlow<PlayerMedia?>
                get() = this@PlayerService.prev
            override val bufferedPosition: StateFlow<Duration>
                get() = this@PlayerService.player.bufferedPosition
            override val actualStartOffset: StateFlow<Duration>
                get() = this@PlayerService.player.actualStartOffset
            override val currentPlayerTime: StateFlow<Duration>
                get() = this@PlayerService.player.currentPlayerTime
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

            override fun getPlayerView(context: Context): PlayerView = this@PlayerService.player.getPlayerView(context)

            override fun playNext(): Unit =
                this@PlayerService.next.value?.let {
                    MainScope().launch {
                        onNext(this@PlayerService.player)
                    }
                    Unit
                } ?: Unit

            override fun playPrev() =
                this@PlayerService.prev.value?.let {
                    MainScope().launch {
                        onPrev(this@PlayerService.player)
                    }
                    Unit
                } ?: Unit

            override fun stop() = this@PlayerService.stop()

            override fun pause() = this@PlayerService.player.pause()

            override fun play() = this@PlayerService.player.play()

            override fun seek(desired: Duration) = this@PlayerService.player.seek(desired)

            override suspend fun loadMedia(mediaInfo: PlayerMedia) = this@PlayerService.player.load(mediaInfo)
        }

    private val mediaSessionCallback =
        object : MediaSession.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                // TODO make user able to disable media buttons via a preference.
                // Disabling media buttons can be done by not calling the super method here, and returning true instead.
                return super.onMediaButtonEvent(mediaButtonIntent)
            }

//            override fun onStop() = this@PlayerService.stop()

            override fun onPlay() = this@PlayerService.player.play()

            override fun onPause() = this@PlayerService.player.pause()

            override fun onFastForward() = this@PlayerService.player.seekForward()

            override fun onRewind() = this@PlayerService.player.seekBack()

            override fun onSkipToNext() =
                runBlocking {
                    val next = this@PlayerService.next.value
                    if (next != null) {
                        this@PlayerService.player.load(next)
                    }
                }

            override fun onSkipToPrevious() =
                runBlocking {
                    val prev = this@PlayerService.prev.value
                    if (prev != null) {
                        this@PlayerService.player.load(prev)
                    }
                }

            override fun onCustomAction(action: String, extras: Bundle?) {
                super.onCustomAction(action, extras)
                when (action) {
                    CUSTOM_ACTION_STOP -> { this@PlayerService.stop() }
                    else -> {}
                }
            }
        }
}
