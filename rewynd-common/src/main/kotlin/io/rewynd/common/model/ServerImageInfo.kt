package io.rewynd.common.model

import io.rewynd.common.SerializableInstant
import kotlinx.serialization.Serializable

@Serializable
data class ServerImageInfo(
    val location: FileLocation,
    val size: Long,
    val libraryId: String,
    val lastUpdated: SerializableInstant,
    val imageId: String,
)
