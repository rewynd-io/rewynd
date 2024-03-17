package io.rewynd.common.model

import io.rewynd.model.NormalizationProps
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class StreamProps(
    val id: String,
    val mediaInfo: ServerMediaInfo,
    val audioStreamName: String?,
    val videoStreamName: String?,
    val subtitleStreamName: String?,
    val normalization: NormalizationProps?,
    val startOffset: Duration = Duration.ZERO,
)
