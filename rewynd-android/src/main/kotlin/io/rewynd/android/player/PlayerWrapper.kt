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
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.ui.PlayerView
import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.android.MILLIS_PER_SECOND
import io.rewynd.android.model.PlayerMedia
import io.rewynd.android.player.StreamHeartbeat.Companion.copy
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.Progress
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import okhttp3.OkHttpClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class PlayerState(
    val bufferedPosition: Duration = Duration.ZERO,
    val currentPlayerTime: Duration = Duration.ZERO,
    val isLoading: Boolean = true,
    val media: PlayerMedia? = null,
    val next: PlayerMedia? = null,
    val prev: PlayerMedia? = null,
    val actualStartOffset: Duration = Duration.ZERO,
    val isPlaying: Boolean = true,
    val playbackState: PlaybackState = PlaybackState.Idle
) {
    val offsetTime = actualStartOffset + currentPlayerTime

    sealed interface PlaybackState {
        data object Unknown : PlaybackState
        data object Ended : PlaybackState
        data object Buffering : PlaybackState
        data object Idle : PlaybackState
        data object Ready : PlaybackState
    }

    companion object {
        val DEFAULT = PlayerState()
    }
}

private fun <T> MutableStateFlow<T>.updateAndGet(block: (T) -> T): T {
    val v = block(value)
    value = v
    return v
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerWrapper(
    context: Context,
    httpClient: OkHttpClient,
    val client: RewyndClient,
    val onEvent: (state: PlayerState) -> Unit = {},
) {
    private val datasourceFactory by lazy { OkHttpDataSource.Factory(httpClient) }

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState>
        get() = _state

    fun MutableStateFlow<PlayerState>.reload(desired: Duration? = null) {
        updateAndGet { playerState ->
            playerState.copy(
                media = playerState.media?.copy(startOffset = desired ?: playerState.offsetTime),
                actualStartOffset = desired ?: playerState.offsetTime,
                currentPlayerTime = Duration.ZERO
            ).also { it.media?.let(this@PlayerWrapper::load) }
        }
    }

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    datasourceFactory,
                    DefaultExtractorsFactory()
                        .setMp4ExtractorFlags(Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)
                        .setFragmentedMp4ExtractorFlags(FragmentedMp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)
                )
            )
            .build().apply {
                addListener(listener)
                addAnalyticsListener(EventLogger())
                playWhenReady = _state.value.isPlaying
            }
    }

    fun getState() = _state.updateAndGet {
        it.copy(
            currentPlayerTime = player.currentPosition.milliseconds,
            bufferedPosition = player.bufferedPosition.milliseconds,
            playbackState = when (player.playbackState) {
                Player.STATE_BUFFERING -> PlayerState.PlaybackState.Buffering
                Player.STATE_READY -> PlayerState.PlaybackState.Ready
                Player.STATE_IDLE -> PlayerState.PlaybackState.Idle
                Player.STATE_ENDED -> PlayerState.PlaybackState.Ended
                else -> PlayerState.PlaybackState.Unknown
            },
            isPlaying = player.isPlaying
        )
    }

    private val listener =
        object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                super.onEvents(player, events)
                log.info { "TimeUpdate: ${player.currentPosition}" }
                val state = getState()
                onEvent(state)
                if (state.media != null && state.offsetTime >= state.media.runTime.minus(100.milliseconds)) {
                    runBlocking {
                        next(startAtZero = true)
                    }
                }
            }

            override fun onPlayerError(e: PlaybackException) =
                runBlocking {
                    log.error(e) { "Player Error!" }
                    _state.reload()
                }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.updateAndGet {
                    it.copy(isPlaying = isPlaying)
                }
            }
        }

    @OptIn(UnstableApi::class)
    fun loadUri(uri: Uri) {
        player.setMediaItem(
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
                .build()
        )
        player.prepare()
    }

    fun next(startAtZero: Boolean = false) {
        _state.updateAndGet {
            it.copy(
                media = it.next?.run {
                    if (startAtZero) {
                        copy(startOffset = Duration.ZERO)
                    } else {
                        this
                    }
                },
                prev = it.media,
                next = runBlocking { it.next?.let { nonNullNext -> PlaybackMethodHandler.next(client, nonNullNext) } },
                actualStartOffset = Duration.ZERO,
                currentPlayerTime = Duration.ZERO
            )
        }.let {
            it.media?.let(this@PlayerWrapper::load) ?: Unit
        }
    }

    fun prev(startAtZero: Boolean = false) {
        _state.updateAndGet {
            it.copy(
                media = it.prev?.run {
                    if (startAtZero) {
                        copy(startOffset = Duration.ZERO)
                    } else {
                        this
                    }
                },
                prev = runBlocking { it.next?.let { nonNullNext -> PlaybackMethodHandler.prev(client, nonNullNext) } },
                next = it.media,
                actualStartOffset = Duration.ZERO,
                currentPlayerTime = Duration.ZERO
            )
        }.let {
            it.media?.let(this@PlayerWrapper::load) ?: Unit
        }
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

    fun load(playerMedia: PlayerMedia) {
        stop()
        runBlocking {
            _state.updateAndGet {
                runBlocking {
                    if (playerMedia != it.media) {
                        it.copy(
                            next = PlaybackMethodHandler.next(client, playerMedia),
                            prev = PlaybackMethodHandler.prev(client, playerMedia),
                            isLoading = true,
                            media = playerMedia
                        )
                    } else {
                        it.copy(isLoading = true, media = playerMedia)
                    }
                }
            }.let(onEvent)

            heartbeat.load(playerMedia.toCreateStreamRequest())
        }
    }

    @OptIn(UnstableApi::class) // HlsMediaSource is unstable
    private val heartbeat by lazy {
        StreamHeartbeat(
            client = client,
            onCanceled = {
                log.info { "Heartbeat Cancelled" }
                _state.updateAndGet {
                    it.copy(
                        media = it.media?.copy(startOffset = it.offsetTime),
                        actualStartOffset = it.offsetTime,
                        currentPlayerTime = Duration.ZERO
                    )
                }.media?.toCreateStreamRequest()
            },
            onAvailable = { _, actualStartOffset ->
                log.info { "Heartbeat Available" }
                putProgress()
                _state.updateAndGet {
                    it.copy(actualStartOffset = actualStartOffset)
                }
            },
        ) { streamProps ->
            log.info { "Heartbeat Loading" }

            val uri = Uri.parse(client.baseUrl + streamProps.url)
            log.info { "Loading media: $uri" }

            loadUri(uri)
            _state.updateAndGet {
                it.copy(isLoading = false)
            }
        }
    }

    private fun putProgress() =
        MainScope().launch {
            val s = _state.value
            when (val m = s.media) {
                null -> {}
                is PlayerMedia.Episode -> {
                    client.putUserProgress(
                        Progress(
                            m.info.id,
                            (s.offsetTime.inWholeMilliseconds / MILLIS_PER_SECOND.toDouble()) / m.info.runTime,
                            Clock.System.now(),
                        ),
                    ).result().onFailure { log.error(it) { "Failed to putUserProgress" } }
                }

                is PlayerMedia.Movie -> {
                    client.putUserProgress(
                        Progress(
                            m.info.id,
                            (s.offsetTime.inWholeMilliseconds / MILLIS_PER_SECOND.toDouble()) / m.info.runTime,
                            Clock.System.now(),
                        ),
                    ).result().onFailure { log.error(it) { "Failed to putUserProgress" } }
                }
            }
        }

    fun seek(desired: Duration) =
        _state.value.media?.let { playerMedia ->
            if (
                desired > playerMedia.startOffset &&
                desired < playerMedia.startOffset + this.player.duration.milliseconds
            ) {
                this.player.seekTo((desired - playerMedia.startOffset).inWholeMilliseconds)
            } else {
                _state.reload(desired)
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
