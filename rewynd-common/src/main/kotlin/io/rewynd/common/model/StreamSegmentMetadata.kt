package io.rewynd.common.model

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class StreamSegmentMetadata(
    val duration: Duration,
)
