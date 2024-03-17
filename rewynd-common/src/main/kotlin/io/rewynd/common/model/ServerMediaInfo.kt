package io.rewynd.common.model

import io.rewynd.model.MediaInfo
import kotlinx.serialization.Serializable

@Serializable
data class ServerMediaInfo(
    val mediaInfo: MediaInfo,
    val libraryData: LibraryData,
    val fileInfo: FileInfo,
    val videoTracks: Map<String, ServerVideoTrack>,
    val audioTracks: Map<String, ServerAudioTrack>,
    val subtitleTracks: Map<String, ServerSubtitleTrack>,
    val subtitleFiles: Map<String, FileLocation>,
)
