package io.rewynd.android.player

import android.util.Log
import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.android.model.PlayerMedia
import io.rewynd.client.RewyndClient
import io.rewynd.model.CreateStreamRequest
import io.rewynd.model.HlsStreamProps
import io.rewynd.model.NormalizationMethod
import io.rewynd.model.StreamStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration

class StreamHeartbeat(
    private val client: RewyndClient,
    private val onCanceled: (CreateStreamRequest) -> CreateStreamRequest? = { it },
    private val onAvailable: (HlsStreamProps) -> Unit = {},
    private val onLoad: (HlsStreamProps) -> Unit,
) {
    private var job: Job? = null
    private val callMutex = Mutex()

    suspend fun load(request: CreateStreamRequest) =
        callMutex.withLock {
            job?.cancel()
            job = startJob(request)
        }

    suspend fun unload() =
        callMutex.withLock {
            job?.cancel()
        }

    private fun startJob(createStreamRequest: CreateStreamRequest) =
        MainScope().launch {
            val props = client.createStream(createStreamRequest).body()
            var lastStatus: StreamStatus? = StreamStatus.Pending
            while (true) {
                try {
                    lastStatus = beat(props, createStreamRequest, lastStatus)
                } catch (e: CancellationException) {
                    withContext(NonCancellable) {
                        client.deleteStream(props.id)
                    }
                    log.warn(e) { "Heartbeat stopped" }
                    return@launch
                } catch (e: Exception) {
                    log.error(e) { "Heartbeat error" }
                }
            }
        }

    @Suppress("MagicNumber")
    private suspend fun beat(
        props: HlsStreamProps,
        lastCreateStreamRequest: CreateStreamRequest,
        priorStatus: StreamStatus?,
    ): StreamStatus? {
        val latestStatus =
            kotlin.runCatching { client.heartbeatStream(props.id).body() }
                .onFailure {
                    log.error(it) { "Failed to heartbeat stream" }
                }
                .getOrNull()
        when (latestStatus) {
            StreamStatus.Available -> {
                if (priorStatus != null && priorStatus != StreamStatus.Available) {
                    onLoad(props)
                }
                onAvailable(props)
                delay(10000)
            }

            StreamStatus.Canceled -> {
                MainScope().launch {
                    onCanceled(lastCreateStreamRequest)?.let {
                        load(it)
                    }
                }
                delay(15000)
            }

            StreamStatus.Pending -> {
                delay(500)
            }

            null -> {
                Log.w("PlayerService", "Stream failed to heartbeat!")
                delay(5000)
            }
        }
        return latestStatus
    }

    companion object {
        private val log by lazy { KotlinLogging.logger { } }

        fun PlayerMedia.copy(
            startOffset: Duration = this.startOffset,
            videoTrackName: String? = this.videoTrackName,
            audioTrackName: String? = this.audioTrackName,
            subtitleTrackName: String? = this.subtitleTrackName,
            normalizationMethod: NormalizationMethod? = this.normalizationMethod,
        ) = when (this) {
            is PlayerMedia.Episode ->
                this.copy(
                    playbackMethod = playbackMethod,
                    startOffset = startOffset,
                    videoTrackName = videoTrackName,
                    subtitleTrackName = subtitleTrackName,
                    audioTrackName = audioTrackName,
                    normalizationMethod = normalizationMethod,
                )
        }
    }
}
