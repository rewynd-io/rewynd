package io.rewynd.common.model

import io.rewynd.model.Progress
import kotlinx.serialization.Serializable

@Serializable
data class UserProgress(
    val username: String,
    val id: String,
    val percent: Double,
    val timestamp: Double,
) {
    fun toProgress() =
        Progress(
            id = id,
            percent = percent,
            timestamp = timestamp,
        )
}
