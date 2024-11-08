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
import androidx.media3.ui.PlayerView
import io.rewynd.android.R
import io.rewynd.android.browser.BrowserActivity
import io.rewynd.android.client.cookie.CookieStorageCookieJar
import io.rewynd.android.client.cookie.PersistentCookiesStorage
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.android.model.PlayerMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class PlayerService : Service() {
    private var browserState: Bundle? = null
    private var originalPlayerProps: PlayerProps? = null
    private val cookies by lazy { PersistentCookiesStorage }
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .cookieJar(CookieStorageCookieJar(cookies))
            .addInterceptor(logging)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val player by lazy {
        PlayerWrapper(this, httpClient, mkRewyndClient(), onEvent = {
            // Relies on the stop() setting instance.value to null, which isn't great
            if (_instance.value != null) {
                setPlaybackState(it)
                createNotification(it)
            }
        })
    }

    private val mediaSession: MediaSession by lazy { MediaSession(this, "RewyndMediaSession") }

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
                player.next()
            }

            is PlayerServiceProps.Prev -> {
                player.prev()
            }
        }

        return START_STICKY
    }

    private fun handleStartIntent(props: PlayerServiceProps.Start) {
        if (props.interruptPlayback || player.state.value.media == null) {
            this.originalPlayerProps = props.playerProps
            runBlocking {
                val playerMedia = props.playerProps.media
                player.load(playerMedia)
            }
            this.mediaSession.setCallback(mediaSessionCallback)
            this.setPlaybackState(player.state.value)
            _instance.value = serviceInterface
        }
    }

    private fun setPlaybackState(state: PlayerState) {
        val playPause =
            if (state.isPlaying) {
                PlaybackState.ACTION_PAUSE
            } else {
                PlaybackState.ACTION_PLAY
            }

        val next =
            if (player.state.value.next == null) {
                0
            } else {
                PlaybackState.ACTION_SKIP_TO_NEXT
            }

        val prev =
            if (player.state.value.prev == null) {
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
                    PlaybackState.ACTION_STOP or
                    PlaybackState.ACTION_SEEK_TO
            ).setState(
                when (state.playbackState) {
                    is PlayerState.PlaybackState.Buffering -> PlaybackState.STATE_BUFFERING
                    is PlayerState.PlaybackState.Idle -> PlaybackState.STATE_PAUSED
                    is PlayerState.PlaybackState.Ready ->
                        if (state.isPlaying) {
                            PlaybackState.STATE_PLAYING
                        } else {
                            PlaybackState.STATE_PAUSED
                        }

                    is PlayerState.PlaybackState.Ended -> PlaybackState.STATE_PAUSED
                    is PlayerState.PlaybackState.Unknown -> PlaybackState.STATE_NONE
                },
                state.offsetTime.inWholeMilliseconds,
                1.0f,
            ).setBufferedPosition(state.bufferedPosition.inWholeMilliseconds)
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
                .putString(MediaMetadata.METADATA_KEY_TITLE, state.media?.title)
                // Artist.
                // Could also be the channel name or TV series.
                .putString(MediaMetadata.METADATA_KEY_ARTIST, state.media?.artist)
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
                    state.media
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
    private fun createNotification(playerState: PlayerState): Unit = playerState.run {
        media?.let { playerMedia ->
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
                        this@PlayerService,
                        PLAYER_SERVICE_NOTIFICATION_CHANNEL_ID,
                    )
                } else {
                    Notification.Builder(this@PlayerService)
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
                    offsetTime.inWholeSeconds.toInt(),
                    false,
                )
                .addAction(
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
                    if (isPlaying) {
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


            startForeground(PLAYER_SERVICE_NOTIFICATION_ID, builder.build())
        } ?: Unit
    }

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
            override val playerState: Flow<PlayerState>
                get() = this@PlayerService.player.state

            override fun getState() = player.getState()

            override fun getPlayerView(context: Context): PlayerView = this@PlayerService.player.getPlayerView(context)

            override fun playNext(): Unit = this@PlayerService.player.next()

            override fun playPrev() = this@PlayerService.player.prev()

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

            override fun onSeekTo(pos: Long) = this@PlayerService.player.seek(pos.milliseconds)

            override fun onStop() = this@PlayerService.stop()

            override fun onPlay() = this@PlayerService.player.play()

            override fun onPause() = this@PlayerService.player.pause()

            override fun onFastForward() = this@PlayerService.player.seekForward()

            override fun onRewind() = this@PlayerService.player.seekBack()

            override fun onSkipToNext() = this@PlayerService.player.next()

            override fun onSkipToPrevious() = this@PlayerService.player.prev()

            override fun onCustomAction(action: String, extras: Bundle?) {
                super.onCustomAction(action, extras)
                when (action) {
                    CUSTOM_ACTION_STOP -> { this@PlayerService.stop() }
                    else -> {}
                }
            }
        }
}
