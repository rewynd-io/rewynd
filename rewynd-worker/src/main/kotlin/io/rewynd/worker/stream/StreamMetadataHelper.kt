package io.rewynd.worker.stream

import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.JobId
import io.rewynd.common.model.Mime
import io.rewynd.common.model.StreamMetadata
import io.rewynd.common.model.StreamMetadataWrapper
import io.rewynd.common.model.StreamProps
import io.rewynd.common.model.StreamSegmentMetadata
import io.rewynd.common.model.SubtitleMetadata
import io.rewynd.common.model.SubtitleSegment
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class StreamMetadataHelper(
    private val streamProps: StreamProps,
    private val jobId: JobId,
    private val cache: Cache,
) {
    private val mutex = Mutex()
    private var subtitles: SubtitleMetadata? = if (streamProps.subtitleStreamName != null) SubtitleMetadata() else null
    private var segments: List<StreamSegmentMetadata> = emptyList()
    private var mime: Mime? = null
    private var complete: Boolean = false
    private var processed: Duration = Duration.ZERO

    private suspend fun put() {
        mime?.let {
            val metadata =
                StreamMetadata(
                    streamProps,
                    segments,
                    subtitles,
                    it,
                    complete,
                    processed,
                    jobId,
                )
            cache.putStreamMetadata(
                streamProps.id,
                StreamMetadataWrapper(
                    metadata,
                ),
                expiration = Clock.System.now() + 1.hours,
            )
        }
    }

    suspend fun addSubtitleSegment(segment: SubtitleSegment) =
        mutex.withLock {
            this.subtitles =
                SubtitleMetadata(
                    segments = (this.subtitles?.segments ?: emptyList()) + listOf(segment),
                    complete = this.subtitles?.complete ?: false,
                )
            put()
        }

    suspend fun completeSubtitles() =
        mutex.withLock {
            this.subtitles = this.subtitles?.copy(complete = true)
            put()
        }

    suspend fun addSegment(duration: Duration) =
        mutex.withLock {
            val index = this.segments.size
            this.segments += listOf(StreamSegmentMetadata(duration))
            this.processed += duration
            put()
            index
        }

    suspend fun init(mime: Mime) =
        mutex.withLock {
            this.mime = mime
            put()
        }

    suspend fun complete() =
        mutex.withLock {
            this.complete = true
            put()
        }

    companion object
}
