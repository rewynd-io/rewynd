package io.rewynd.common.model

import io.rewynd.common.SerializableInstant
import io.rewynd.common.toAudioTracks
import io.rewynd.common.toSubtitleTracks
import io.rewynd.common.toVideoTracks
import io.rewynd.model.MediaInfo
import io.rewynd.model.MovieInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class ServerMovieInfo(
    val id: String,
    val libraryId: String,
    val title: String,
    val runTime: Double,
    val plot: String? = null,
    val outline: String? = null,
    val directors: List<String>? = null,
    val writers: List<String>? = null,
    val credits: List<String>? = null,
    val studios: List<String>? = null,
    val rating: Double? = null,
    val criticRating: Int? = null,
    val mpaa: String? = null,
    val premiered: LocalDate? = null,
    val tagLine: String? = null,
    val country: String? = null,
    val genre: String? = null,
    val releaseDate: LocalDate? = null,
    val year: Int? = null,
    val backdropImageId: String? = null,
    val posterImageId: String? = null,
    val lastModified: SerializableInstant = Clock.System.now(),
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
            directors = directors,
            writers = writers,
            credits = credits,
            rating = rating,
            year = year,
            backdropImageId = backdropImageId,
            posterImageId = posterImageId,
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
