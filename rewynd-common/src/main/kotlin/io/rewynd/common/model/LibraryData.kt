package io.rewynd.common.model

import io.rewynd.common.SerializableInstant
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
data class LibraryData(
    val libraryId: String,
    val lastUpdated: SerializableInstant = Clock.System.now(),
)
