package io.rewynd.common.model

import io.rewynd.common.SerializableInstant
import io.rewynd.model.Actor
import io.rewynd.model.ShowInfo
import kotlinx.serialization.Serializable

@Serializable
data class ServerShowInfo(
    val id: String,
    val libraryId: String,
    val title: String,
    val plot: String? = null,
    val outline: String? = null,
    val originalTitle: String? = null,
    val premiered: String? = null,
    val releaseDate: String? = null,
    val endDate: String? = null,
    val mpaa: String? = null,
    val imdbId: String? = null,
    val tmdbId: String? = null,
    val tvdbId: String? = null,
    val tvRageId: String? = null,
    val rating: Double? = null,
    val year: Double? = null,
    val runTime: Double? = null,
    val aired: Double? = null,
    val genre: String? = null,
    val studio: String? = null,
    val status: String? = null,
    val tag: List<String>? = null,
    val actors: List<Actor>? = null,
    val seriesImageId: String? = null,
    val backdropImageId: String? = null,
    val lastUpdated: SerializableInstant,
) {
    fun toShowInfo() =
        ShowInfo(
            id = id,
            libraryId = libraryId,
            title = title,
            plot = plot,
            outline = outline,
            originalTitle = originalTitle,
            premiered = premiered,
            releaseDate = releaseDate,
            endDate = endDate,
            mpaa = mpaa,
            imdbId = imdbId,
            tmdbId = tmdbId,
            tvdbId = tvdbId,
            tvRageId = tvRageId,
            rating = rating,
            year = year,
            runTime = runTime,
            aired = aired,
            genre = genre,
            studio = studio,
            status = status,
            tag = tag,
            actors = actors,
            seriesImageId = seriesImageId,
            backdropImageId = backdropImageId,
        )
}
