package io.rewynd.common.model

import io.rewynd.common.cache.queue.JobId
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class StreamMetadata(
    val streamProps: StreamProps,
    val segments: List<StreamSegmentMetadata>,
    val subtitles: SubtitleMetadata?,
    val mime: Mime,
    val complete: Boolean,
    val processed: Duration,
    val jobId: JobId,
)
