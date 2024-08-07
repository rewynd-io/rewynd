package io.rewynd.common.model

import io.rewynd.common.toAudioTracks
import io.rewynd.common.toSubtitleTracks
import io.rewynd.common.toVideoTracks
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.MediaInfo
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class ServerEpisodeInfo(
    val id: String,
    val libraryId: String,
    val showId: String,
    val seasonId: String,
    val title: String,
    val runTime: Double,
    val plot: String? = null,
    val outline: String? = null,
    val director: List<String>? = null,
    val writer: List<String>? = null,
    val credits: List<String>? = null,
    val rating: Double? = null,
    val year: Int? = null,
    val episode: Int,
    val episodeNumberEnd: Int? = null,
    val season: Int,
    val showName: String,
    val aired: LocalDate? = null,
    val episodeImageId: String? = null,
    val fileInfo: FileInfo,
    val videoTracks: Map<String, ServerVideoTrack>,
    val audioTracks: Map<String, ServerAudioTrack>,
    val subtitleTracks: Map<String, ServerSubtitleTrack>,
    val subtitleFileTracks: Map<String, SubtitleFileTrack>,
    val lastModified: Instant,
    val lastUpdated: Instant,
) : Comparable<ServerEpisodeInfo> {
    fun toEpisodeInfo() =
        EpisodeInfo(
            id = id,
            libraryId = libraryId,
            audioTracks = audioTracks.toAudioTracks(),
            videoTracks = videoTracks.toVideoTracks(),
            subtitleTracks =
            subtitleTracks.toSubtitleTracks() +
                subtitleFileTracks.mapValues { it.value.track }
                    .toSubtitleTracks(),
            showId = showId,
            seasonId = seasonId,
            title = title,
            runTime = runTime,
            plot = plot,
            outline = outline,
            director = director,
            writer = writer,
            credits = credits,
            rating = rating,
            year = year,
            episode = episode,
            episodeNumberEnd = episodeNumberEnd,
            season = season,
            showName = showName,
            aired = aired,
            episodeImageId = episodeImageId,
            lastUpdated = lastUpdated,
            lastModified = lastModified
        )

    fun toServerMediaInfo(): ServerMediaInfo =
        ServerMediaInfo(
            mediaInfo =
            MediaInfo(
                id = id,
                libraryId = libraryId,
                audioTracks = audioTracks.toAudioTracks(),
                videoTracks = videoTracks.toVideoTracks(),
                subtitleTracks =
                subtitleTracks.toSubtitleTracks() +
                    subtitleFileTracks.mapValues { it.value.track }
                        .toSubtitleTracks(),
                runTime = runTime,
            ),
            libraryData =
            LibraryData(
                libraryId = libraryId,
                lastUpdated = lastUpdated,
            ),
            fileInfo = fileInfo,
            audioTracks = audioTracks,
            videoTracks = videoTracks,
            subtitleTracks = subtitleTracks + subtitleFileTracks.mapValues { it.value.track },
            subtitleFiles = subtitleFileTracks.mapValues { it.value.location },
        )

    override fun compareTo(other: ServerEpisodeInfo): Int =
        this.episode.compareTo(other.episode)
}
