package io.rewynd.common.model

import io.rewynd.common.SerializableInstant
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class LibraryData(
    val libraryId: String,
    val lastUpdated: SerializableInstant = Clock.System.now(),
)
