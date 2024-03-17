package io.rewynd.common.model

import kotlinx.serialization.Serializable

@Serializable
data class FileInfo(val location: FileLocation, val size: Long)
