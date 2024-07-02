package io.rewynd.worker.stream

import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.JobContext
import io.rewynd.common.cache.queue.StreamJobHandler
import io.rewynd.common.model.ClientStreamEvents
import io.rewynd.common.model.StreamProps
import io.rewynd.common.model.WorkerStreamEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val log by lazy { KotlinLogging.logger { } }

fun mkStreamJobHandler(cache: Cache): StreamJobHandler =
    { context ->
        val originalStreamProps = context.request
        val streamProps =
            originalStreamProps.copy(
                startOffset =
                if (originalStreamProps.videoTrack?.canCopy == true) {
                    findPriorKeyframe(originalStreamProps)
                } else {
                    originalStreamProps.startOffset
                },
            )
        log.info { "Requested: ${originalStreamProps.startOffset}, starting at: ${streamProps.startOffset}" }

        val metadataHelper = StreamMetadataHelper(streamProps, context.jobId, cache)

        val subtitleJob =
            launchSubtitleJob(streamProps, metadataHelper)

        val job =
            launchMediaJob(
                streamProps,
                context,
                metadataHelper,
                cache,
            )

        val heartbeat =
            launchHeartbeatJob(streamProps, context)

        try {
            log.info { "Started stream ${streamProps.id}" }
            job.join()
        } catch (e: Exception) {
            log.error(e) { "Error waiting for stream job to finish" }
            cache.cleanupStream(streamProps)
            throw e
        } finally {
            job.cancel()
            subtitleJob.cancel()
            log.info { "Cancelling heartbeat" }
            heartbeat.cancel()
            log.info { "Canceled heartbeat" }
        }
    }

private suspend fun Cache.cleanupStream(streamProps: StreamProps) =
    withContext(NonCancellable) {
        getStreamMetadata(streamProps.id)?.streamMetadata?.segments?.indices?.forEach {
            delSegmentM4s(streamProps.id, it)
        }
        delStreamMetadata(streamProps.id)
        delInitMp4(streamProps.id)
    }

private fun CoroutineScope.launchHeartbeatJob(
    streamProps: StreamProps,
    context: JobContext<StreamProps, Unit, ClientStreamEvents, WorkerStreamEvents>,
) = launch(Dispatchers.IO) {
    log.info { "Started heartbeat for ${streamProps.id}" }
    val heartbeatState =
        context.clientEvents
            .takeWhile { isActive }
            .map {
                log.info { "Stream ${streamProps.id} got heartbeat" }
                Instant.now()
            }.stateIn(this)
    while (isActive &&
        heartbeatState.value.isAfter(
            Instant.now().minus(30.seconds.toJavaDuration()),
        )
    ) {
        log.info { "Stream ${streamProps.id} last heartbeat ${heartbeatState.value}" }
        delay(1.seconds)
    }
    log.info { "Killing ${streamProps.id} due to failure to heartbeat" }
    throw CancellationException("Heartbeat failed")
}
