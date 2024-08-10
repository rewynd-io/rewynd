package io.rewynd.android.model

import io.rewynd.android.util.details
import io.rewynd.model.AudioTrack
import io.rewynd.model.CreateStreamRequest
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.MovieInfo
import io.rewynd.model.NormalizationMethod
import io.rewynd.model.NormalizationProps
import io.rewynd.model.SubtitleTrack
import io.rewynd.model.VideoTrack
import kotlin.time.Duration

@kotlinx.serialization.Serializable
sealed interface PlayerMedia {
    val startOffset: Duration
    val runTime: Duration
    val subtitleTrackName: String?
    val videoTrackName: String?
    val audioTrackName: String?
    val normalizationMethod: NormalizationMethod?

    @kotlinx.serialization.Serializable
    data class Episode(
        val playbackMethod: EpisodePlaybackMethod,
        val info: EpisodeInfo,
        override val runTime: Duration,
        override val startOffset: Duration = Duration.ZERO,
        override val subtitleTrackName: String?,
        override val videoTrackName: String?,
        override val audioTrackName: String?,
        override val normalizationMethod: NormalizationMethod?,
    ) : PlayerMedia {
        @kotlinx.serialization.Serializable
        sealed interface EpisodePlaybackMethod {
            @kotlinx.serialization.Serializable
            data object Sequential : EpisodePlaybackMethod
        }
    }


    @kotlinx.serialization.Serializable
    data class Movie(
        val info: MovieInfo,
        override val runTime: Duration,
        override val startOffset: Duration = Duration.ZERO,
        override val subtitleTrackName: String?,
        override val videoTrackName: String?,
        override val audioTrackName: String?,
        override val normalizationMethod: NormalizationMethod?,
    ) : PlayerMedia

    val title: String
        get() =
            when (this) {
                is Episode -> info.title
                is Movie -> info.title
            }

    val artist: String?
        get() =
            when (this) {
                is Episode -> info.showName
                is Movie -> null
            }

    val details: String
        get() =
            when (this) {
                is Episode -> info.details
                is Movie -> info.title
            }

    val audioTracks: Map<String, AudioTrack>
        get() =
            when (this) {
                is Episode -> info.audioTracks
                is Movie -> info.audioTracks
            }
    val videoTracks: Map<String, VideoTrack>
        get() =
            when (this) {
                is Episode -> info.videoTracks
                is Movie -> info.videoTracks
            }
    val subtitleTracks: Map<String, SubtitleTrack>
        get() =
            when (this) {
                is Episode -> info.subtitleTracks
                is Movie -> info.subtitleTracks
            }

    fun toCreateStreamRequest() =
        when (this) {
            is Episode -> {
                CreateStreamRequest(
                    library = this.info.libraryId,
                    id = this.info.id,
                    audioTrack = this.audioTrackName,
                    videoTrack = this.videoTrackName,
                    subtitleTrack = this.subtitleTrackName,
                    startOffset = (this.startOffset.inWholeMilliseconds / 1000).toDouble(),
                    normalization = this.normalizationMethod?.let { NormalizationProps(it) },
                )
            }
            is Movie -> {
                CreateStreamRequest(
                    library = this.info.libraryId,
                    id = this.info.id,
                    audioTrack = this.audioTrackName,
                    videoTrack = this.videoTrackName,
                    subtitleTrack = this.subtitleTrackName,
                    startOffset = (this.startOffset.inWholeMilliseconds / 1000).toDouble(),
                    normalization = this.normalizationMethod?.let { NormalizationProps(it) },
                )
            }
        }
}
