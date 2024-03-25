package io.rewynd.common.database

import io.rewynd.common.KLog
import io.rewynd.common.config.DatabaseConfig
import io.rewynd.common.model.LibraryIndex
import io.rewynd.common.model.ServerEpisodeInfo
import io.rewynd.common.model.ServerImageInfo
import io.rewynd.common.model.ServerMovieInfo
import io.rewynd.common.model.ServerScheduleInfo
import io.rewynd.common.model.ServerSeasonInfo
import io.rewynd.common.model.ServerShowInfo
import io.rewynd.common.model.ServerUser
import io.rewynd.common.model.SessionStorage
import io.rewynd.common.model.UserProgress
import io.rewynd.model.Library
import io.rewynd.model.ListEpisodesByLastUpdatedOrder
import io.rewynd.model.Progress
import kotlinx.datetime.Instant

sealed interface Database {
    suspend fun init()

    suspend fun getUser(username: String): ServerUser?

    suspend fun upsertUser(user: ServerUser): Boolean

    suspend fun deleteUser(username: String): Boolean

    suspend fun listUsers(): List<ServerUser>

    suspend fun getLibrary(libraryId: String): Library?

    suspend fun upsertLibrary(lib: Library): Boolean

    suspend fun deleteLibrary(libraryId: String): Boolean

    suspend fun listLibraries(): List<Library>

    suspend fun getSchedule(scheduleId: String): ServerScheduleInfo?

    suspend fun upsertSchedule(schedule: ServerScheduleInfo): Boolean

    suspend fun deleteSchedule(scheduleId: String): Boolean

    suspend fun listSchedules(): List<ServerScheduleInfo>

    suspend fun getShow(showId: String): ServerShowInfo?

    suspend fun upsertShow(show: ServerShowInfo): Boolean

    suspend fun deleteShow(showId: String): Boolean

    suspend fun listShows(libraryId: String): List<ServerShowInfo>

    suspend fun getSeason(seasonId: String): ServerSeasonInfo?

    suspend fun upsertSeason(season: ServerSeasonInfo): Boolean

    suspend fun deleteSeason(seasonId: String): Boolean

    suspend fun listSeasons(showId: String): List<ServerSeasonInfo>

    suspend fun getEpisode(episodeId: String): ServerEpisodeInfo?

    suspend fun upsertEpisode(episode: ServerEpisodeInfo): Boolean

    suspend fun deleteEpisode(episodeId: String): Boolean

    suspend fun listEpisodes(
        seasonId: String,
        cursor: String? = null,
    ): List<ServerEpisodeInfo>

    suspend fun listEpisodesByLastUpdated(
        cursor: Long?,
        order: ListEpisodesByLastUpdatedOrder,
    ): List<ServerEpisodeInfo>

    suspend fun getMovie(movieId: String): ServerMovieInfo?

    suspend fun upsertMovie(movieInfo: ServerMovieInfo): Boolean

    suspend fun deleteMovie(movieId: String): Boolean

    suspend fun listMovies(libraryId: String): List<ServerMovieInfo>

    suspend fun getImage(imageId: String): ServerImageInfo?

    suspend fun upsertImage(imageInfo: ServerImageInfo): Boolean

    suspend fun deleteImage(imageId: String): Boolean

    suspend fun listImages(libraryId: String): List<ServerImageInfo>

    suspend fun mkSessionStorage(): SessionStorage

    suspend fun cleanShows(
        start: Instant,
        libraryId: String,
    ): Int

    suspend fun cleanSeasons(
        start: Instant,
        libraryId: String,
    ): Int

    suspend fun cleanEpisodes(
        start: Instant,
        libraryId: String,
    ): Int

    suspend fun cleanImages(
        start: Instant,
        libraryId: String,
    ): Int

    suspend fun getLibraryIndex(
        libraryId: String,
        updatedAfter: Instant? = null,
    ): LibraryIndex?

    suspend fun upsertLibraryIndex(index: LibraryIndex): Boolean

    suspend fun deleteLibraryIndex(libraryId: String): Boolean

    suspend fun listLibraryIndexes(): List<LibraryIndex>

    suspend fun getProgress(
        id: String,
        username: String,
    ): UserProgress?

    suspend fun upsertProgress(progress: UserProgress): Boolean

    suspend fun deleteProgress(
        id: String,
        username: String,
    ): Boolean

    suspend fun listRecentProgress(
        username: String,
        cursor: Progress? = null,
        minPercent: Double = 0.0,
        maxPercent: Double = 1.0,
    ): List<UserProgress>

    companion object : KLog() {
        fun fromConfig(config: DatabaseConfig) =
            when (config) {
                is DatabaseConfig.PostgresConfig -> PostgresDatabase(config)
            }
    }
}
