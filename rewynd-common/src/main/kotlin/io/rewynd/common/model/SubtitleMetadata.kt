package io.rewynd.common.model

import kotlinx.serialization.Serializable

@Serializable
data class SubtitleMetadata(
    val segments: List<SubtitleSegment> = emptyList(),
    val complete: Boolean = false,
)
