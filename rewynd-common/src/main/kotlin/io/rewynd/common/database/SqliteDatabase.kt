package io.rewynd.common.database

import io.rewynd.common.config.DatabaseConfig
import io.rewynd.common.config.datasource
import io.rewynd.common.model.LibraryIndex
import io.rewynd.common.model.Progressed
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.Database as Connection

class SqliteDatabase(
    config: DatabaseConfig.SqliteConfig,
    private val conn: Connection = Connection.connect(config.datasource),
) : SqlDatabase(conn) {
    private val mutex = Mutex()

    override suspend fun getUser(username: String): ServerUser? =
        mutex.withLock {
            super.getUser(username)
        }

    override suspend fun upsertUser(user: ServerUser): Boolean =
        mutex.withLock {
            super.upsertUser(user)
        }

    override suspend fun deleteUser(username: String): Boolean =
        mutex.withLock {
            super.deleteUser(username)
        }

    override suspend fun listUsers(cursor: String?): List<ServerUser> =
        mutex.withLock {
            super.listUsers(cursor)
        }

    override suspend fun getLibrary(libraryId: String): Library? =
        mutex.withLock {
            super.getLibrary(libraryId)
        }

    override suspend fun upsertLibrary(lib: Library): Boolean =
        mutex.withLock {
            super.upsertLibrary(lib)
        }

    override suspend fun deleteLibrary(libraryId: String): Boolean =
        mutex.withLock {
            super.deleteLibrary(libraryId)
        }

    override suspend fun listLibraries(cursor: String?): List<Library> =
        mutex.withLock {
            super.listLibraries(cursor)
        }

    override suspend fun getShow(showId: String): ServerShowInfo? =
        mutex.withLock {
            super.getShow(showId)
        }

    override suspend fun upsertShow(show: ServerShowInfo): Boolean =
        mutex.withLock {
            super.upsertShow(show)
        }

    override suspend fun deleteShow(showId: String): Boolean =
        mutex.withLock {
            super.deleteShow(showId)
        }

    override suspend fun listShows(
        libraryId: String,
        cursor: String?,
    ): List<ServerShowInfo> =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            super.listShows(libraryId, cursor)
        }

    override suspend fun getSeason(seasonId: String): ServerSeasonInfo? =
        mutex.withLock {
            super.getSeason(seasonId)
        }

    override suspend fun upsertSeason(season: ServerSeasonInfo): Boolean =
        mutex.withLock {
            super.upsertSeason(season)
        }

    override suspend fun deleteSeason(seasonId: String): Boolean =
        mutex.withLock {
            super.deleteSeason(seasonId)
        }

    override suspend fun listSeasons(
        showId: String,
        cursor: String?,
    ): List<ServerSeasonInfo> =
        mutex.withLock {
            super.listSeasons(showId, cursor)
        }

    override suspend fun getEpisode(episodeId: String): ServerEpisodeInfo? =
        mutex.withLock {
            super.getEpisode(episodeId)
        }

    override suspend fun getProgressedEpisode(
        episodeId: String,
        username: String
    ): Progressed<ServerEpisodeInfo>? =
        mutex.withLock {
            super.getProgressedEpisode(episodeId, username)
        }

    override suspend fun upsertEpisode(episode: ServerEpisodeInfo): Boolean =
        mutex.withLock {
            super.upsertEpisode(episode)
        }

    override suspend fun deleteEpisode(episodeId: String): Boolean =
        mutex.withLock {
            super.deleteEpisode(episodeId)
        }

    override suspend fun listProgressedEpisodes(
        seasonId: String,
        cursor: String?,
        username: String
    ): Paged<Progressed<ServerEpisodeInfo>, String> =
        mutex.withLock {
            super.listProgressedEpisodes(seasonId, cursor, username)
        }

    override suspend fun listProgressedEpisodesByLastUpdated(
        cursor: String?,
        limit: Int,
        libraryIds: List<String>,
        username: String
    ): Paged<Progressed<ServerEpisodeInfo>, String> =
        mutex.withLock {
            super.listProgressedEpisodesByLastUpdated(cursor, limit, libraryIds, username)
        }

    override suspend fun listNextEpisodes(
        username: String,
        cursor: Long?
    ): Paged<Progressed<ServerEpisodeInfo>, Long> =
        mutex.withLock {
            super.listNextEpisodes(username, cursor)
        }

    override suspend fun getProgressedMovie(
        movieId: String,
        username: String
    ): Progressed<ServerMovieInfo>? =
        mutex.withLock {
            super.getProgressedMovie(movieId, username)
        }

    override suspend fun getMovie(movieId: String): ServerMovieInfo? =
        mutex.withLock {
            super.getMovie(movieId)
        }

    override suspend fun upsertMovie(movieInfo: ServerMovieInfo): Boolean =
        mutex.withLock {
            super.upsertMovie(movieInfo)
        }

    override suspend fun deleteMovie(movieId: String): Boolean =
        mutex.withLock {
            super.deleteMovie(movieId)
        }

    override suspend fun listProgressedMovies(
        libraryId: String,
        cursor: String?,
        username: String
    ): Paged<Progressed<ServerMovieInfo>, String> =
        mutex.withLock {
            super.listProgressedMovies(libraryId, cursor, username)
        }

    override suspend fun getSchedule(scheduleId: String): ServerScheduleInfo? =
        mutex.withLock {
            super.getSchedule(scheduleId)
        }

    override suspend fun upsertSchedule(schedule: ServerScheduleInfo): Boolean =
        mutex.withLock {
            super.upsertSchedule(schedule)
        }

    override suspend fun deleteSchedule(scheduleId: String): Boolean =
        mutex.withLock {
            super.deleteSchedule(scheduleId)
        }

    override suspend fun listSchedules(cursor: String?): List<ServerScheduleInfo> =
        mutex.withLock {
            super.listSchedules(cursor)
        }

    override suspend fun getImage(imageId: String): ServerImageInfo? =
        mutex.withLock {
            super.getImage(imageId)
        }

    override suspend fun upsertImage(imageInfo: ServerImageInfo): Boolean =
        mutex.withLock {
            super.upsertImage(imageInfo)
        }

    override suspend fun deleteImage(imageId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun listImages(libraryId: String): List<ServerImageInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun mkSessionStorage(): SessionStorage {
        val superStorage = super.mkSessionStorage()
        return object : SessionStorage {
            override suspend fun invalidate(id: String) =
                mutex.withLock {
                    superStorage.invalidate(id)
                }

            override suspend fun write(
                id: String,
                value: String,
            ) = mutex.withLock {
                superStorage.write(id, value)
            }

            override suspend fun read(id: String): String =
                mutex.withLock {
                    superStorage.read(id)
                }
        }
    }

    override suspend fun cleanMovies(
        start: Instant,
        libraryId: String,
    ) = mutex.withLock {
        super.cleanMovies(start, libraryId)
    }

    override suspend fun cleanShows(
        start: Instant,
        libraryId: String,
    ) = mutex.withLock {
        super.cleanShows(start, libraryId)
    }

    override suspend fun cleanSeasons(
        start: Instant,
        libraryId: String,
    ) = mutex.withLock {
        super.cleanSeasons(start, libraryId)
    }

    override suspend fun cleanEpisodes(
        start: Instant,
        libraryId: String,
    ) = mutex.withLock {
        super.cleanEpisodes(start, libraryId)
    }

    override suspend fun cleanImages(
        start: Instant,
        libraryId: String,
    ) = mutex.withLock {
        super.cleanImages(start, libraryId)
    }

    override suspend fun getLibraryIndex(
        libraryId: String,
        updatedAfter: Instant?,
    ): LibraryIndex? =
        mutex.withLock {
            super.getLibraryIndex(libraryId, updatedAfter)
        }

    override suspend fun upsertLibraryIndex(index: LibraryIndex): Boolean =
        mutex.withLock {
            super.upsertLibraryIndex(index)
        }

    override suspend fun deleteLibraryIndex(libraryId: String): Boolean =
        mutex.withLock {
            super.deleteLibraryIndex(libraryId)
        }

    override suspend fun listLibraryIndexes(): List<LibraryIndex> {
        TODO("Not yet implemented")
    }

    override suspend fun getProgress(
        id: String,
        username: String,
    ): UserProgress? =
        mutex.withLock {
            super.getProgress(id, username)
        }

    override suspend fun upsertProgress(progress: UserProgress): Boolean =
        mutex.withLock {
            super.upsertProgress(progress)
        }

    override suspend fun deleteProgress(
        id: String,
        username: String,
    ): Boolean =
        mutex.withLock {
            super.deleteProgress(id, username)
        }

    override suspend fun listRecentProgress(
        username: String,
        cursor: Instant?,
        minPercent: Double,
        maxPercent: Double,
        limit: Int,
    ): List<UserProgress> =
        mutex.withLock {
            super.listRecentProgress(username, cursor, minPercent, maxPercent, limit)
        }
}
