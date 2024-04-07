package io.rewynd.worker.ffprobe

import io.rewynd.common.model.ServerAudioTrack
import io.rewynd.common.model.ServerSubtitleTrack
import io.rewynd.common.model.ServerVideoTrack

data class FfprobeInfo(
    val audioTracks: Map<String, ServerAudioTrack>,
    val videoTracks: Map<String, ServerVideoTrack>,
    val subtitleTracks: Map<String, ServerSubtitleTrack>,
    val runTime: Double,
)