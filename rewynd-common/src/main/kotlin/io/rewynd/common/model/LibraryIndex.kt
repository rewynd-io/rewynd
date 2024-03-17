package io.rewynd.common.model

import io.rewynd.common.SerializableInstant
import kotlinx.serialization.Serializable

@Serializable
data class LibraryIndex(
    val index: ByteArray,
    val libraryId: String,
    val lastUpdated: SerializableInstant,
)
