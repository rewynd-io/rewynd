package io.rewynd.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface WorkerStreamEvents {
    @Serializable
    @SerialName("Start")
    data object Start : WorkerStreamEvents

    @Serializable
    @SerialName("Progress")
    data class Progress(val status: Map<String, String>) : WorkerStreamEvents
}
