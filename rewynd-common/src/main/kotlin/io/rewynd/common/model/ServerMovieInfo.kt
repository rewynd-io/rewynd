package io.rewynd.common.model

import io.rewynd.common.SerializableInstant
import io.rewynd.common.toAudioTracks
import io.rewynd.common.toSubtitleTracks
import io.rewynd.common.toVideoTracks
import io.rewynd.model.MediaInfo
import io.rewynd.model.MovieInfo
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class ServerMovieInfo(
    val id: String,
    val libraryId: String,
    val title: String,
    val runTime: Double,
    val plot: String? = null,
    val outline: String? = null,
    val director: String? = null,
    val writer: List<String>? = null,
    val credits: List<String>? = null,
    val studios: List<String>? = null,
    val rating: Double? = null,
    val criticRating: Double? = null,
    val mpaa: Double? = null,
    val premiered: String? = null,
    val tagLine: String? = null,
    val country: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val year: Double? = null,
    val episodeImageId: String? = null,
    val lastUpdated: SerializableInstant = Clock.System.now(),
    val fileInfo: FileInfo,
    val videoTracks: Map<String, ServerVideoTrack>,
    val audioTracks: Map<String, ServerAudioTrack>,
    val subtitleTracks: Map<String, ServerSubtitleTrack>,
    val subtitleFileTracks: Map<String, SubtitleFileTrack>,
) {
    fun toMovieInfo() =
        MovieInfo(
            id = id,
            libraryId = libraryId,
            audioTracks = audioTracks.toAudioTracks(),
            videoTracks = videoTracks.toVideoTracks(),
            subtitleTracks =
            subtitleTracks.toSubtitleTracks() +
                subtitleFileTracks.mapValues { it.value.track }
                    .toSubtitleTracks(),
            title = title,
            runTime = runTime,
            plot = plot,
            outline = outline,
            director = director,
            writer = writer,
            credits = credits,
            rating = rating,
            year = year,
            episodeImageId = episodeImageId,
            studios = studios,
            criticRating = criticRating,
            mpaa = mpaa,
            premiered = premiered,
            tagLine = tagLine,
            country = country,
            genre = genre,
            releaseDate = releaseDate,
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
}
