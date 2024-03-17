package io.rewynd.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface FileLocation {
    @Serializable
    @SerialName("LocalFile")
    data class LocalFile(val path: String) : FileLocation
}
