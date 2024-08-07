package io.rewynd.android.player

import android.content.Context
import android.net.Uri
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
import androidx.media3.ui.PlayerView
import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.android.MILLIS_PER_SECOND
import io.rewynd.android.model.PlayerMedia
import io.rewynd.android.player.StreamHeartbeat.Companion.copy
import io.rewynd.client.RewyndClient
import io.rewynd.model.Progress
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import okhttp3.OkHttpClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PlayerWrapper(
    context: Context,
    httpClient: OkHttpClient,
    val client: RewyndClient,
    val onEvent: () -> Unit = {},
    onNext: suspend (player: PlayerWrapper) -> Unit = {},
) {
    private val datasourceFactory by lazy { OkHttpDataSource.Factory(httpClient) }

    val bufferedPosition: MutableStateFlow<Duration> = MutableStateFlow(0.milliseconds)
    val currentPlayerTime: MutableStateFlow<Duration> = MutableStateFlow(0.milliseconds)
    val isLoading: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val playbackState: MutableStateFlow<Int> = MutableStateFlow(Player.STATE_IDLE)
    val media: MutableStateFlow<PlayerMedia?> = MutableStateFlow(null)
    val actualStartOffset: MutableStateFlow<Duration> = MutableStateFlow(Duration.ZERO)
    val isPlayingState by lazy { MutableStateFlow(player.playWhenReady) }

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            addListener(listener)
            addAnalyticsListener(EventLogger())
            playWhenReady = true
        }
    }

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
                val m = media.value
                if (m != null && currentOffsetTime >= m.runTime.minus(1.seconds)) {
                    runBlocking {
                        onNext(this@PlayerWrapper)
                    }
                }
                onEvent()
            }

            override fun onPlayerError(e: PlaybackException) =
                runBlocking {
                    log.error(e) { "Player Error!" }
                    media.value?.copy(currentPlayerTime.value)
                        ?.let { this@PlayerWrapper.load(it) } ?: Unit
                }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                runBlocking {
                    isPlayingState.value = isPlaying
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                this@PlayerWrapper.playbackState.value = playbackState
                runBlocking {
                    when (playbackState) {
                        Player.STATE_ENDED -> {}
                        Player.STATE_BUFFERING -> {}
                        Player.STATE_IDLE -> {}
                        Player.STATE_READY -> {}
                    }
                    Unit
                }
            }
        }

    @OptIn(UnstableApi::class)
    fun loadUri(uri: Uri) {
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
    }

    fun stop() =
        runBlocking {
            player.stop()
            heartbeat.unload()
        }

    fun pause() {
        player.pause()
    }

    fun play() {
        player.play()
    }

    fun load(playerMedia: PlayerMedia) =
        runBlocking {
            this@PlayerWrapper.isLoading.value = true
            media.value = playerMedia

            heartbeat.load(playerMedia.toCreateStreamRequest())
            this@PlayerWrapper.onEvent()
        }

    @OptIn(UnstableApi::class) // HlsMediaSource is unstable
    private val heartbeat by lazy {
        StreamHeartbeat(
            client = client,
            onCanceled = {
                log.info { "Heartbeat Cancelled" }
                it.copy(startOffset = currentOffsetTime.inWholeMilliseconds / 1000.0)
            },
            onAvailable = { _, actualStartOffset ->
                log.info { "Heartbeat Available" }
                putProgress()
                this.actualStartOffset.value = actualStartOffset
            },
        ) {
            log.info { "Heartbeat Loading" }

            val uri = Uri.parse(client.baseUrl + it.url)
            log.info { "Loading media: $uri" }

            loadUri(uri)
            this.isLoading.value = false
        }
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
                                (currentOffsetTime.inWholeMilliseconds / MILLIS_PER_SECOND.toDouble()) / m.info.runTime,
                                Clock.System.now(),
                            ),
                        )
                    }.onFailure { log.error(it) { "Failed to putUserProgress" } }
                }
            }
        }

    val currentOffsetTime: Duration
        get() = this.currentPlayerTime.value + (actualStartOffset.value)

    fun seek(desired: Duration) =
        this.media.value?.let { playerMedia ->
            if (
                desired > playerMedia.startOffset &&
                desired < playerMedia.startOffset + this.player.duration.milliseconds
            ) {
                this.player.seekTo((desired - playerMedia.startOffset).inWholeMilliseconds)
            } else {
                runBlocking {
                    this@PlayerWrapper.actualStartOffset.value = desired
                    this@PlayerWrapper.load(playerMedia.copy(startOffset = desired))
                }
            }
        } ?: Unit

    fun getPlayerView(context: Context): PlayerView =
        PlayerView(context).apply {
            this.player = this@PlayerWrapper.player
        }

    fun seekBack() = player.seekBack()

    fun seekForward() = player.seekForward()

    companion object {
        private val log = KotlinLogging.logger { }
    }
}
