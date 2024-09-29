package io.rewynd.common.model

import io.rewynd.common.toAudioTracks
import io.rewynd.common.toSubtitleTracks
import io.rewynd.common.toVideoTracks
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.MediaInfo
import io.rewynd.model.Progress
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
    override fun compareTo(other: ServerEpisodeInfo): Int {
        val seasonComp = this.season.compareTo(other.season)
        return if (seasonComp == 0) {
            this.episode.compareTo(other.episode)
        } else {
            seasonComp
        }
    }

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
}

fun Progressed<ServerEpisodeInfo>.toEpisodeInfo() =
    EpisodeInfo(
        id = data.id,
        libraryId = data.libraryId,
        audioTracks = data.audioTracks.toAudioTracks(),
        videoTracks = data.videoTracks.toVideoTracks(),
        subtitleTracks =
        data.subtitleTracks.toSubtitleTracks() +
            data.subtitleFileTracks.mapValues { it.value.track }
                .toSubtitleTracks(),
        showId = data.showId,
        seasonId = data.seasonId,
        title = data.title,
        runTime = data.runTime,
        plot = data.plot,
        outline = data.outline,
        director = data.director,
        writer = data.writer,
        credits = data.credits,
        rating = data.rating,
        year = data.year,
        episode = data.episode,
        episodeNumberEnd = data.episodeNumberEnd,
        season = data.season,
        showName = data.showName,
        aired = data.aired,
        episodeImageId = data.episodeImageId,
        lastUpdated = data.lastUpdated,
        lastModified = data.lastModified,
        progress = progress?.toProgress() ?: Progress(data.id, 0.0, Instant.DISTANT_PAST)
    )
