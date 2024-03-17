package io.rewynd.common.model

import kotlinx.serialization.Serializable

@Serializable
data class SubtitleFileTrack(val location: FileLocation, val track: ServerSubtitleTrack)
