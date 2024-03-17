package io.rewynd.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ClientStreamEvents {
    @Serializable
    @SerialName("Heartbeat")
    data object Heartbeat : ClientStreamEvents
}
