package io.rewynd.common.database

import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.common.JSON
import io.rewynd.common.database.Database.Companion.LIST_EPISODES_MAX_SIZE
import io.rewynd.common.generateSalt
import io.rewynd.common.hashPassword
import io.rewynd.common.model.FileInfo
import io.rewynd.common.model.LibraryData
import io.rewynd.common.model.LibraryIndex
import io.rewynd.common.model.Progressed
import io.rewynd.common.model.ServerAudioTrack
import io.rewynd.common.model.ServerEpisodeInfo
import io.rewynd.common.model.ServerImageInfo
import io.rewynd.common.model.ServerMovieInfo
import io.rewynd.common.model.ServerScheduleInfo
import io.rewynd.common.model.ServerSeasonInfo
import io.rewynd.common.model.ServerShowInfo
import io.rewynd.common.model.ServerSubtitleTrack
import io.rewynd.common.model.ServerUser
import io.rewynd.common.model.ServerVideoTrack
import io.rewynd.common.model.SessionStorage
import io.rewynd.common.model.UserProgress
import io.rewynd.common.toSql
import io.rewynd.model.Library
import io.rewynd.model.LibraryType
import io.rewynd.model.ListNewEpisodesCursor
import io.rewynd.model.ListStartedEpisodesCursor
import io.rewynd.model.SeasonInfo
import io.rewynd.model.User
import io.rewynd.model.UserPermissions
import io.rewynd.model.UserPreferences
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.Alias
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.LiteralOp
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.coalesce
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.eqSubQuery
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.upsert
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import java.util.UUID
import kotlin.time.Instant
import org.jetbrains.exposed.v1.jdbc.Database as Connection

open class SqlDatabase(
    private val conn: Connection,
) : Database {
    override suspend fun init() {
        suspendTransaction(conn) {
            MigrationUtils.statementsRequiredForDatabaseMigration(
                Users,
                Sessions,
                Libraries,
                Movies,
                Shows,
                Seasons,
                Episodes,
                Progression,
                LibraryIndices,
                Schedules,
                withLogs = true
            )
        }

        if (listAllUsers().firstOrNull() == null) {
            val username = "rewynd-${UUID.randomUUID()}"
            val salt = generateSalt()
            val password = UUID.randomUUID().toString()
            val hashedPass = hashPassword(password, salt)
            runBlocking {
                upsertUser(
                    ServerUser(
                        User(
                            username,
                            UserPermissions(isAdmin = true),
                            UserPreferences(false),
                        ),
                        hashedPass,
                        salt,
                    ),
                )
            }
            log.info { "Created user '$username' with password '$password'" }
        }
    }

    override suspend fun getUser(username: String): ServerUser? =
        suspendTransaction(conn) {
            Users
                .selectAll()
                .where { Users.username eq username }
                .limit(1)
                .firstOrNull()
                ?.toServerUser()
        }

    override suspend fun upsertUser(user: ServerUser): Boolean =
        suspendTransaction(conn) {
            Users
                .upsert(Users.username) {
                    it[username] = user.user.username
                    it[hashedPassword] = user.hashedPass
                    it[preferences] = JSON.encodeToString(user.user.preferences)
                    it[permissions] = JSON.encodeToString(user.user.permissions)
                    it[salt] = user.salt
                }.insertedCount == 1
        }

    override suspend fun deleteUser(username: String): Boolean =
        suspendTransaction(conn) {
            Users.deleteWhere {
                Users.username eq username
            } == 1
        }

    override suspend fun listUsers(cursor: String?): List<ServerUser> =
        suspendTransaction(conn) {
            Users
                .selectAll()
                .let {
                    if (cursor != null) {
                        it.where { Users.username greater cursor }
                    } else {
                        it
                    }
                }.orderBy(Users.username, SortOrder.ASC)
                .map {
                    it.toServerUser()
                }
        }

    override suspend fun getLibrary(libraryId: String): Library? =
        suspendTransaction(conn) {
            Libraries.selectAll().where { Libraries.libraryId eq libraryId }.firstOrNull()?.let {
                Library(
                    name = it[Libraries.libraryId],
                    type = it[Libraries.type],
                    rootPaths = JSON.decodeFromString<List<String>>(it[Libraries.rootPaths]),
                )
            }
        }

    override suspend fun upsertLibrary(lib: Library): Boolean =
        suspendTransaction(conn) {
            Libraries
                .upsert(Libraries.libraryId) {
                    it[libraryId] = lib.name
                    it[type] = lib.type
                    it[rootPaths] = JSON.encodeToString(lib.rootPaths)
                }.insertedCount == 1
        }

    override suspend fun deleteLibrary(libraryId: String): Boolean =
        suspendTransaction(conn) {
            Libraries.deleteWhere {
                Libraries.libraryId eq libraryId
            } == 1
        }

    override suspend fun listLibraries(cursor: String?): List<Library> =
        suspendTransaction(conn) {
            Libraries
                .selectAll()
                .let {
                    if (cursor != null) {
                        it.where { Libraries.libraryId greater cursor }
                    } else {
                        it
                    }
                }.orderBy(Libraries.libraryId, SortOrder.ASC)
                .map {
                    Library(
                        name = it[Libraries.libraryId],
                        type = it[Libraries.type],
                        rootPaths = JSON.decodeFromString<List<String>>(it[Libraries.rootPaths]),
                    )
                }
        }

    override suspend fun getShow(showId: String): ServerShowInfo? =
        suspendTransaction(conn) {
            Shows
                .selectAll()
                .where { Shows.showId eq showId }
                .firstOrNull()
                ?.toServerShowInfo()
        }

    override suspend fun upsertShow(show: ServerShowInfo): Boolean =
        suspendTransaction(conn) {
            Shows
                .upsert(Shows.showId) {
                    it[showId] = show.id
                    it[libraryId] = show.libraryId
                    it[title] = show.title
                    it[plot] = show.plot
                    it[outline] = show.outline
                    it[originalTitle] = show.originalTitle
                    it[premiered] = show.premiered?.toString()
                    it[releaseDate] = show.releaseDate?.toString()
                    it[endDate] = show.endDate?.toString()
                    it[mpaa] = show.mpaa
                    it[imdbId] = show.imdbId
                    it[tmdbId] = show.tmdbId
                    it[tvdbId] = show.tvdbId
                    it[tvRageId] = show.tvRageId
                    it[rating] = show.rating
                    it[year] = show.year
                    it[runTime] = show.runTime
                    it[aired] = show.aired?.toString()
                    it[genre] = show.genre
                    it[studio] = show.studio
                    it[status] = show.status
                    it[tag] = show.tag?.let(JSON::encodeToString)
                    it[actors] = show.actors?.let(JSON::encodeToString)
                    it[seriesImageId] = show.seriesImageId
                    it[backdropImageId] = show.backdropImageId
                    it[lastUpdated] = show.lastUpdated.toEpochMilliseconds()
                }.insertedCount == 1
        }

    override suspend fun deleteShow(showId: String): Boolean =
        suspendTransaction(conn) {
            Shows.deleteWhere { Shows.showId eq showId } == 1
        }

    override suspend fun listShows(
        libraryId: String,
        cursor: String?,
    ): List<ServerShowInfo> =
        suspendTransaction(conn) {
            Shows
                .selectAll()
                .where {
                    if (cursor == null) {
                        Shows.libraryId eq libraryId
                    } else {
                        (Shows.libraryId eq libraryId) and (Shows.showId greater cursor)
                    }
                }.orderBy(Shows.showId, SortOrder.ASC)
                .map { it.toServerShowInfo() }
        }

    override suspend fun getSeason(seasonId: String): ServerSeasonInfo? =
        suspendTransaction(conn) {
            Seasons
                .selectAll()
                .where { Seasons.seasonId eq seasonId }
                .firstOrNull()
                ?.toServerSeasonInfo()
        }

    override suspend fun upsertSeason(season: ServerSeasonInfo): Boolean =
        suspendTransaction(conn) {
            Seasons
                .upsert(Seasons.seasonId) {
                    it[seasonId] = season.seasonInfo.id
                    it[showId] = season.seasonInfo.showId
                    it[seasonNumber] = season.seasonInfo.seasonNumber
                    it[libraryId] = season.libraryData.libraryId
                    it[showName] = season.seasonInfo.showName
                    it[year] = season.seasonInfo.year
                    it[premiered] = season.seasonInfo.premiered?.toString()
                    it[releaseDate] = season.seasonInfo.releaseDate?.toString()
                    it[folderImageId] = season.seasonInfo.folderImageId
                    it[actors] = season.seasonInfo.actors?.let(JSON::encodeToString)
                    it[libraryId] = season.libraryData.libraryId
                    it[lastUpdated] = season.libraryData.lastUpdated.toEpochMilliseconds()
                }.insertedCount == 1
        }

    override suspend fun deleteSeason(seasonId: String): Boolean =
        suspendTransaction(conn) {
            Seasons.deleteWhere { Seasons.seasonId eq seasonId } == 1
        }

    override suspend fun listSeasons(
        showId: String,
        cursor: String?,
    ): List<ServerSeasonInfo> =
        suspendTransaction(conn) {
            Seasons
                .selectAll()
                .where {
                    if (cursor == null) {
                        Seasons.showId eq showId
                    } else {
                        (Seasons.showId eq showId) and (Seasons.seasonId greater cursor)
                    }
                }.orderBy(Seasons.seasonId, SortOrder.ASC)
                .map { it.toServerSeasonInfo() }
        }

    override suspend fun getEpisode(episodeId: String): ServerEpisodeInfo? =
        suspendTransaction(conn) {
            Episodes
                .selectAll()
                .where { Episodes.episodeId eq episodeId }
                .firstOrNull()
                ?.toServerEpisodeInfo()
        }

    override suspend fun getProgressedEpisode(episodeId: String, username: String): Progressed<ServerEpisodeInfo>? =
        suspendTransaction(conn) {
            val userProgress = Progression.selectAll().where {
                Progression.username eq username
            }.alias("userProgress")

            Episodes.leftJoin(userProgress, { Episodes.episodeId }, { userProgress[Progression.mediaId] })
                .selectAll()
                .where {
                    Episodes.episodeId eq episodeId
                }
                .firstOrNull()
                ?.toProgressedServerEpisodeInfo()
        }

    override suspend fun listNextEpisodes(
        cursor: Long?,
        username: String,
        completedPercent: Double,
        notStartedPercent: Double
    ): Paged<Progressed<ServerEpisodeInfo>, Long> = suspendTransaction(conn) {
        val current = Episodes.alias("current")
        val currProg = Progression.alias("currProg")
        val next = Episodes.alias("next")
        val nextProg = Progression.alias("nextProg")

        val progressedCurrent =
            current.leftJoin(currProg) { current[Episodes.episodeId] eq currProg[Progression.mediaId] }
        val progressedNext = next.leftJoin(nextProg) { next[Episodes.episodeId] eq nextProg[Progression.mediaId] }

        val innerQuery = Episodes.select(
            Episodes.episodeId
        ).where {
            (Episodes.showId eq current[Episodes.showId]) and
                (Episodes.episodeId neq current[Episodes.episodeId]) and
                (
                    (Episodes.season greater current[Episodes.season]) or
                        (
                            (Episodes.season eq current[Episodes.season]) and
                                (Episodes.episode greater current[Episodes.episode])
                            )
                    )
        }.orderBy(Episodes.season to SortOrder.ASC, Episodes.episode to SortOrder.ASC).limit(1)

        progressedCurrent.crossJoin(progressedNext).select(next.columns + nextProg.columns + currProg.columns).where {
            (if (cursor == null) Op.TRUE else currProg[Progression.timestamp] less cursor) and
                (currProg[Progression.username] eq username) and
                ((nextProg[Progression.username].isNull()) or (nextProg[Progression.username] eq username)) and
                (currProg[Progression.percent] greater completedPercent) and
                (
                    (
                        coalesce(
                            nextProg[Progression.percent],
                            LiteralOp(Progression.percent.columnType, 0.0)
                        ) less notStartedPercent
                        ) or
                        (
                            coalesce(
                                nextProg[Progression.timestamp],
                                LiteralOp(Progression.timestamp.columnType, 0)
                            ) less currProg[Progression.timestamp]
                            )
                    ) and
                (next[Episodes.episodeId] eqSubQuery innerQuery)
        }.orderBy(currProg[Progression.timestamp], SortOrder.DESC).limit(LIST_EPISODES_MAX_SIZE).let { results ->
            val lastTimestamp = results.lastOrNull()?.get(currProg[Progression.timestamp])
            Paged(results.map { it.toProgressedServerEpisodeInfo(next, nextProg) }, lastTimestamp)
        }
    }

    override suspend fun getNextProgressedEpisode(
        episodeId: String,
        order: io.rewynd.model.SortOrder,
        username: String
    ): Progressed<ServerEpisodeInfo>? =
        suspendTransaction(conn) {
            val current =
                Episodes.select(
                    Episodes.showId,
                    Episodes.season,
                    Episodes.episode,
                    Episodes.episodeId,
                    Episodes.title
                ).where { Episodes.episodeId eq episodeId }
                    .alias("current")
            val progress = Progression.selectAll().where { Progression.username eq username }.alias("progress")
            Episodes.innerJoin(
                current,
                onColumn = { showId },
                otherColumn = { this[Episodes.showId] }
            ).leftJoin(progress, { Episodes.episodeId }, { progress[Progression.mediaId] }).select(Episodes.columns)
                .where {
                    Episodes.episodeId neq current[Episodes.episodeId] and when (order) {
                        io.rewynd.model.SortOrder.Ascending -> {
                            (
                                (Episodes.episode eq current[Episodes.episode]) and
                                    (Episodes.season eq current[Episodes.season]) and
                                    (Episodes.title eq current[Episodes.title]) and
                                    (Episodes.episodeId greater current[Episodes.episodeId])
                                ) or (
                                (Episodes.episode eq current[Episodes.episode]) and
                                    (Episodes.season eq current[Episodes.season]) and
                                    (Episodes.title greater current[Episodes.title])
                                ) or (
                                (Episodes.episode greater current[Episodes.episode]) and
                                    (Episodes.season eq current[Episodes.season])
                                ) or
                                (Episodes.season greater current[Episodes.season])
                        }

                        io.rewynd.model.SortOrder.Descending -> {
                            (
                                (Episodes.episode eq current[Episodes.episode]) and
                                    (Episodes.season eq current[Episodes.season]) and
                                    (Episodes.title eq current[Episodes.title]) and
                                    (Episodes.episodeId less current[Episodes.episodeId])
                                ) or
                                (
                                    (Episodes.episode eq current[Episodes.episode]) and
                                        (Episodes.season eq current[Episodes.season]) and
                                        (Episodes.title less current[Episodes.title])
                                    ) or (
                                    (Episodes.episode less current[Episodes.episode]) and
                                        (Episodes.season eq current[Episodes.season])
                                    ) or
                                (Episodes.season less current[Episodes.season])
                        }
                    }
                }
                .orderBy(
                    Episodes.season to order.toSql(),
                    Episodes.episode to order.toSql(),
                    Episodes.title to order.toSql(),
                    Episodes.id to order.toSql()
                ).limit(1)
                .map { it.toProgressedServerEpisodeInfo() }.firstOrNull()
        }

    override suspend fun upsertEpisode(episode: ServerEpisodeInfo): Boolean =
        suspendTransaction(conn) {
            Episodes
                .upsert(Episodes.episodeId) {
                    it[showId] = episode.showId
                    it[showName] = episode.showName
                    it[seasonId] = episode.seasonId
                    it[episodeId] = episode.id
                    it[location] = episode.fileInfo.location.let(JSON::encodeToString)
                    it[size] = episode.fileInfo.size
                    it[lastUpdated] = episode.lastUpdated.toEpochMilliseconds()
                    it[lastModified] = episode.lastModified.toEpochMilliseconds()
                    it[libraryId] = episode.libraryId
                    it[audioTracks] = episode.audioTracks.let(JSON::encodeToString)
                    it[videoTracks] = episode.videoTracks.let(JSON::encodeToString)
                    it[subtitleTracks] = episode.subtitleTracks.let(JSON::encodeToString)
                    it[subtitleFiles] = episode.subtitleFileTracks.let(JSON::encodeToString)
                    it[title] = episode.title
                    it[runTime] = episode.runTime
                    it[plot] = episode.plot
                    it[outline] = episode.outline
                    it[directors] = episode.director?.let(JSON::encodeToString)
                    it[writers] = episode.writer?.let(JSON::encodeToString)
                    it[credits] = episode.credits?.let(JSON::encodeToString)
                    it[rating] = episode.rating
                    it[year] = episode.year
                    it[Episodes.episode] =
                        episode.episode
                    it[episodeNumberEnd] = episode.episodeNumberEnd
                    it[season] = episode.season
                    it[aired] = episode.aired?.toString()
                    it[episodeImageId] = episode.episodeImageId
                }.insertedCount == 1
        }

    override suspend fun deleteEpisode(episodeId: String): Boolean =
        suspendTransaction(conn) {
            Episodes.deleteWhere {
                Episodes.episodeId eq episodeId
            } == 1
        }

    override suspend fun listProgressedEpisodesByProgress(
        username: String,
        cursor: ListStartedEpisodesCursor?,
        minPercent: Double,
        maxPercent: Double,
        limit: Int
    ): Paged<Progressed<ServerEpisodeInfo>, ListStartedEpisodesCursor> =
        suspendTransaction(conn) {
            val userProgress = Progression.selectAll().where {
                Progression.username eq username
            }.alias("userProgress")

            Episodes.leftJoin(userProgress, { episodeId }, { userProgress[Progression.mediaId] })
                .selectAll()
                .where {
                    (
                        userProgress[Progression.timestamp].isNotNull() and
                            (
                                coalesce(
                                    userProgress[Progression.percent],
                                    LiteralOp(Progression.percent.columnType, 0.0)
                                ) greaterEq minPercent
                                )
                        ) and
                        (
                            coalesce(
                                userProgress[Progression.percent],
                                LiteralOp(Progression.percent.columnType, 0.0)
                            ) lessEq maxPercent
                            )
                }
                .apply {
                    if (cursor != null) {
                        andWhere {
                            (userProgress[Progression.timestamp] less cursor.timestamp.toEpochMilliseconds()) or
                                (
                                    userProgress[Progression.timestamp] eq cursor.timestamp.toEpochMilliseconds() and
                                        (Episodes.episodeId less cursor.episodeId)
                                    )
                        }
                    }
                }
                .orderBy(userProgress[Progression.timestamp] to SortOrder.DESC, Episodes.episodeId to SortOrder.DESC)
                .limit(limit)
                .map { it.toProgressedServerEpisodeInfo() }.let {
                    Paged(
                        it,
                        it.lastOrNull()?.run {
                            ListStartedEpisodesCursor(
                                data.id,
                                (
                                    progress?.timestamp
                                        ?: Instant.DISTANT_PAST
                                    ) // TODO should never be null, probably a better way to handle this
                            )
                        }
                    )
                }
        }

    override suspend fun listProgressedEpisodesByModified(
        username: String,
        cursor: ListNewEpisodesCursor?,
        minPercent: Double,
        maxPercent: Double,
        limit: Int
    ): Paged<Progressed<ServerEpisodeInfo>, ListNewEpisodesCursor> =
        suspendTransaction(conn) {
            val userProgress = Progression.selectAll().where {
                Progression.username eq username
            }.alias("userProgress")

            Episodes.leftJoin(userProgress, { episodeId }, { userProgress[Progression.mediaId] })
                .selectAll()
                .where {
                    (
                        coalesce(
                            userProgress[Progression.percent],
                            LiteralOp(Progression.percent.columnType, 0.0)
                        ) greaterEq minPercent
                        ) and
                        (
                            coalesce(
                                userProgress[Progression.percent],
                                LiteralOp(Progression.percent.columnType, 0.0)
                            ) lessEq maxPercent
                            )
                }
                .apply {
                    if (cursor != null) {
                        andWhere {
                            (Episodes.lastModified less cursor.timestamp.toEpochMilliseconds()) or
                                (
                                    Episodes.lastModified eq cursor.timestamp.toEpochMilliseconds() and
                                        (Episodes.episodeId less cursor.episodeId)
                                    )
                        }
                    }
                }
                .orderBy(Episodes.lastModified to SortOrder.DESC, Episodes.episodeId to SortOrder.DESC)
                .limit(limit)
                .map { it.toProgressedServerEpisodeInfo() }.let {
                    Paged(
                        it,
                        it.lastOrNull()?.run {
                            ListNewEpisodesCursor(data.id, data.lastModified)
                        }
                    )
                }
        }

    override suspend fun listProgressedEpisodes(
        username: String,
        seasonId: String,
        cursor: String?,
        limit: Int
    ): Paged<Progressed<ServerEpisodeInfo>, String> =
        suspendTransaction(conn) {
            val userProgress = Progression.selectAll().where {
                Progression.username eq username
            }.alias("userProgress")

            Episodes.leftJoin(userProgress, { episodeId }, { userProgress[Progression.mediaId] })
                .selectAll()
                .where {
                    (Episodes.seasonId eq seasonId)
                }
                .apply {
                    if (cursor != null) {
                        andWhere {
                            Episodes.episodeId greater cursor
                        }
                    }
                }
                .orderBy(Episodes.episodeId, SortOrder.ASC)
                .limit(limit)
                .map { it.toProgressedServerEpisodeInfo() }.let { Paged(it, it.lastOrNull()?.data?.id) }
        }

    override suspend fun getMovie(movieId: String): ServerMovieInfo? =
        suspendTransaction(conn) {
            Movies
                .selectAll()
                .where { Movies.movieId eq movieId }
                .firstOrNull()
                ?.toServerMovieInfo()
        }

    override suspend fun getProgressedMovie(movieId: String, username: String): Progressed<ServerMovieInfo>? =
        suspendTransaction(conn) {
            val userProgress = Progression.selectAll().where {
                Progression.username eq username
            }.alias("userProgress")

            Movies.leftJoin(userProgress, { Movies.movieId }, { userProgress[Progression.mediaId] })
                .selectAll()
                .where {
                    Movies.movieId eq movieId
                }
                .firstOrNull()
                ?.toProgressedServerMovieInfo()
        }

    override suspend fun upsertMovie(movieInfo: ServerMovieInfo): Boolean =
        suspendTransaction(conn) {
            Movies
                .upsert(Movies.movieId) {
                    it[movieId] = movieInfo.id
                    it[libraryId] = movieInfo.libraryId
                    it[title] = movieInfo.title
                    it[plot] = movieInfo.plot
                    it[outline] = movieInfo.outline
                    it[directors] = movieInfo.directors.let(JSON::encodeToString)
                    it[writers] = movieInfo.writers.let(JSON::encodeToString)
                    it[credits] = movieInfo.credits.let(JSON::encodeToString)
                    it[studios] = movieInfo.studios.let(JSON::encodeToString)
                    it[directors] = movieInfo.directors.let(JSON::encodeToString)
                    it[rating] = movieInfo.rating
                    it[criticRating] = movieInfo.criticRating
                    it[mpaa] = movieInfo.mpaa
                    it[premiered] = movieInfo.premiered?.toString()
                    it[tagline] = movieInfo.tagLine
                    it[runTime] = movieInfo.runTime
                    it[country] = movieInfo.country
                    it[releaseDate] = movieInfo.releaseDate?.toString()
                    it[year] = movieInfo.year
                    it[lastModified] = movieInfo.lastModified.toEpochMilliseconds()
                    it[lastUpdated] = movieInfo.lastUpdated.toEpochMilliseconds()
                    it[location] = movieInfo.fileInfo.location.let(JSON::encodeToString)
                    it[size] = movieInfo.fileInfo.size
                    it[subtitleFiles] = movieInfo.subtitleFileTracks.let(JSON::encodeToString)
                    it[subtitleTracks] = movieInfo.subtitleTracks.let(JSON::encodeToString)
                    it[videoTracks] = movieInfo.videoTracks.let(JSON::encodeToString)
                    it[audioTracks] = movieInfo.audioTracks.let(JSON::encodeToString)
                    it[posterImageId] = movieInfo.posterImageId
                    it[backdropImageId] = movieInfo.backdropImageId
                }.insertedCount == 1
        }

    override suspend fun deleteMovie(movieId: String): Boolean =
        suspendTransaction(conn) {
            Movies.deleteWhere { Movies.movieId eq movieId } == 1
        }

    override suspend fun listProgressedMovies(
        libraryId: String,
        cursor: String?,
        username: String
    ): Paged<Progressed<ServerMovieInfo>, String> =
        suspendTransaction(conn) {
            Movies.leftJoin(Progression, { movieId }, { mediaId })
                .selectAll()
                .let {
                    // TODO min and max progression filters
                    if (cursor != null) {
                        it.where {
                            Movies.movieId greater cursor
                        }
                    } else {
                        it
                    }
                }.orderBy(Movies.movieId, SortOrder.ASC)
                .limit(LIST_EPISODES_MAX_SIZE)
                .map { it.toProgressedServerMovieInfo() }.let { Paged(it, it.lastOrNull()?.data?.id) }
        }

    override suspend fun cleanMovies(
        start: Instant,
        libraryId: String,
    ): Int =
        suspendTransaction(conn) {
            Movies.deleteWhere {
                lastUpdated less start.toEpochMilliseconds() and (
                    Movies.libraryId eq libraryId
                    )
            }
        }

    override suspend fun getSchedule(scheduleId: String): ServerScheduleInfo? =
        suspendTransaction(conn) {
            Schedules
                .selectAll()
                .where { Schedules.scheduleId eq scheduleId }
                .firstOrNull()
                ?.toServerScheduleInfo()
        }

    override suspend fun upsertSchedule(schedule: ServerScheduleInfo): Boolean =
        suspendTransaction(conn) {
            Schedules
                .upsert(Schedules.scheduleId) {
                    it[scheduleId] = schedule.id
                    it[cronExpression] = schedule.cronExpression
                    it[scanTasks] = JSON.encodeToString(schedule.scanTasks)
                }.insertedCount == 1
        }

    override suspend fun deleteSchedule(scheduleId: String): Boolean =
        suspendTransaction(conn) {
            Schedules.deleteWhere {
                Schedules.scheduleId eq scheduleId
            } == 1
        }

    override suspend fun listSchedules(cursor: String?): List<ServerScheduleInfo> =
        suspendTransaction(conn) {
            Schedules
                .selectAll()
                .let {
                    if (cursor != null) {
                        it.where { Schedules.scheduleId greater cursor }
                    } else {
                        it
                    }
                }.orderBy(Schedules.scheduleId, SortOrder.ASC)
                .map {
                    ServerScheduleInfo(
                        id = it[Schedules.scheduleId],
                        cronExpression = it[Schedules.cronExpression],
                        scanTasks = JSON.decodeFromString(it[Schedules.scanTasks]),
                    )
                }
        }

    override suspend fun getImage(imageId: String): ServerImageInfo? =
        suspendTransaction(conn) {
            Images
                .selectAll()
                .where { Images.imageId eq imageId }
                .firstOrNull()
                ?.toServerImageInfo()
        }

    override suspend fun upsertImage(imageInfo: ServerImageInfo): Boolean =
        suspendTransaction(conn) {
            Images
                .upsert(Images.imageId) {
                    it[imageId] = imageInfo.imageId
                    it[size] = imageInfo.size
                    it[lastUpdated] = imageInfo.lastUpdated.toEpochMilliseconds()
                    it[libraryId] = imageInfo.libraryId
                    it[location] = imageInfo.location.let(JSON::encodeToString)
                }.insertedCount == 1
        }

    override suspend fun deleteImage(imageId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun listImages(libraryId: String): List<ServerImageInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun mkSessionStorage(): SessionStorage =
        object : SessionStorage {
            override suspend fun invalidate(id: String) =
                suspendTransaction(conn) {
                    Sessions.deleteWhere {
                        sessionId eq id
                    }
                    Unit
                }

            override suspend fun write(
                id: String,
                value: String,
            ) = suspendTransaction(conn) {
                Sessions.upsert(Sessions.sessionId) {
                    it[sessionId] = id
                    it[Sessions.value] = value
                }
                Unit
            }

            override suspend fun read(id: String): String =
                suspendTransaction(conn) {
                    Sessions
                        .selectAll()
                        .where { Sessions.sessionId eq id }
                        .firstOrNull()
                        ?.getOrNull(Sessions.value)
                        ?: throw NoSuchElementException("Session $id not found")
                }
        }

    override suspend fun cleanShows(
        start: Instant,
        libraryId: String,
    ) = suspendTransaction(conn) {
        Shows.deleteWhere {
            lastUpdated less start.toEpochMilliseconds() and (
                Shows.libraryId eq libraryId
                )
        }
    }

    override suspend fun cleanSeasons(
        start: Instant,
        libraryId: String,
    ) = suspendTransaction(conn) {
        Seasons.deleteWhere {
            lastUpdated less start.toEpochMilliseconds() and (Seasons.libraryId eq libraryId)
        }
    }

    override suspend fun cleanEpisodes(
        start: Instant,
        libraryId: String,
    ) = suspendTransaction(conn) {
        Episodes.deleteWhere {
            lastUpdated less start.toEpochMilliseconds() and (Episodes.libraryId eq libraryId)
        }
    }

    override suspend fun cleanImages(
        start: Instant,
        libraryId: String,
    ) = suspendTransaction(conn) {
        Images.deleteWhere {
            lastUpdated less start.toEpochMilliseconds() and (Images.libraryId eq libraryId)
        }
    }

    override suspend fun getLibraryIndex(
        libraryId: String,
        updatedAfter: Instant?,
    ): LibraryIndex? =
        suspendTransaction(conn) {
            LibraryIndices
                .selectAll()
                .where {
                    if (updatedAfter != null) {
                        LibraryIndices.libraryId eq libraryId and
                            (LibraryIndices.lastUpdated greater updatedAfter.toEpochMilliseconds())
                    } else {
                        LibraryIndices.libraryId eq libraryId
                    }
                }.firstOrNull()
        }?.toLibraryIndex()

    override suspend fun upsertLibraryIndex(index: LibraryIndex): Boolean =
        suspendTransaction(conn) {
            LibraryIndices.upsert(LibraryIndices.libraryId) {
                it[libraryId] = index.libraryId
                it[lastUpdated] = index.lastUpdated.toEpochMilliseconds()
                it[LibraryIndices.index] = ExposedBlob(index.index)
            }
        }.insertedCount == 1

    override suspend fun deleteLibraryIndex(libraryId: String): Boolean =
        suspendTransaction(conn) {
            LibraryIndices.deleteWhere { LibraryIndices.libraryId eq libraryId }
        } == 1

    override suspend fun listLibraryIndexes(): List<LibraryIndex> {
        TODO("Not yet implemented")
    }

    override suspend fun getProgress(
        id: String,
        username: String,
    ): UserProgress? =
        suspendTransaction(conn) {
            Progression
                .selectAll()
                .where { (Progression.mediaId eq id) and (Progression.username eq username) }
                .firstOrNull()
                ?.toProgress()
        }

    override suspend fun upsertProgress(progress: UserProgress): Boolean =
        suspendTransaction(conn) {
            Progression
                .upsert(Progression.mediaId) {
                    it[mediaId] = progress.id
                    it[timestamp] = progress.timestamp.toEpochMilliseconds()
                    it[username] = progress.username
                    it[percent] = progress.percent.coerceAtMost(1.0).coerceAtLeast(0.0)
                }.insertedCount == 1
        }

    override suspend fun deleteProgress(
        id: String,
        username: String,
    ): Boolean =
        suspendTransaction(conn) {
            Progression.deleteWhere {
                (mediaId eq id) and (Progression.username eq username)
            } == 1
        }

    override suspend fun listRecentProgress(
        username: String,
        cursor: Instant?,
        minPercent: Double,
        maxPercent: Double,
        limit: Int,
    ): List<UserProgress> =
        suspendTransaction(conn) {
            Progression
                .selectAll()
                .where {
                    (
                        Progression.percent.lessEq(maxPercent) and
                            Progression.percent.greaterEq(minPercent) and
                            Progression.username.eq(username)
                        ).let {
                        if (cursor != null) {
                            it and Progression.timestamp.less(cursor.toEpochMilliseconds())
                        } else {
                            it
                        }
                    }
                }.orderBy(Progression.timestamp to SortOrder.DESC, Progression.mediaId to SortOrder.DESC)
                .limit(limit)
                .asFlow()
                .map { it.toProgress() }
                .toList()
        }

    object Shows : IntIdTable() {
        val showId = text("show_id").uniqueIndex()

        val libraryId = text("library_id").references(Libraries.libraryId)
        val lastUpdated = long("lastUpdated")

        val title = text("title")
        val plot = text("plot").nullable()
        val outline = text("outline").nullable()
        val originalTitle = text("original_title").nullable()
        val premiered = text("premiered").nullable()
        val releaseDate = text("release_date").nullable()
        val endDate = text("end_date").nullable()
        val mpaa = text("mpaa").nullable()
        val imdbId = text("imdb_id").nullable()
        val tmdbId = text("tmdb_id").nullable()
        val tvdbId = text("tvdb_id").nullable()
        val tvRageId = text("tv_rage_id").nullable()
        val rating = double("rating").nullable()
        val year = integer("year").nullable()
        val runTime = double("run_time").nullable()
        val episode = integer("episode").nullable()
        val episodeNumberEnd = integer("episode_number_end").nullable()
        val season = integer("season").nullable()
        val aired = text("aired").nullable()
        val genre = text("genre").nullable()
        val studio = text("studio").nullable()
        val status = text("status").nullable()
        val tag = text("tag").nullable()
        val actors = text("actors").nullable()
        val seriesImageId = text("series_image_id").references(Images.imageId).nullable()
        val backdropImageId = text("backdrop_image_id").references(Images.imageId).nullable()
    }

    object Episodes : IntIdTable() {
        val showId = text("show_id").references(Shows.showId).index()
        val showName = text("show_name")
        val seasonId = text("season_id").references(Seasons.seasonId)
        val episodeId = text("episode_id").uniqueIndex()

        val location = text("location")
        val size = long("size")

        val lastUpdated = long("last_updated")
        val lastModified = long("last_modified")
        val libraryId = text("library_id").references(Libraries.libraryId)

        val audioTracks = text("audio_tracks")
        val videoTracks = text("video_tracks")
        val subtitleTracks = text("subtitle_tracks")
        val subtitleFiles = text("subtitle_files")

        val title = text("title")
        val runTime = double("run_time")
        val plot = text("plot").nullable()
        val outline = text("outline").nullable()
        val directors = text("directors").nullable()
        val writers = text("writers").nullable()
        val credits = text("credits").nullable()
        val rating = double("rating").nullable()
        val year = integer("year").nullable()
        val episode = integer("episode").index()
        val episodeNumberEnd = integer("premiered").nullable()
        val season = integer("season").index()
        val aired = text("aired").nullable()
        val episodeImageId = text("episode_image_id").references(Images.imageId).nullable()
    }

    object Seasons : IntIdTable() {
        val seasonId = text("season_id").uniqueIndex()
        val seasonNumber = integer("season_number")
        val showId = text("show_id").references(Shows.showId)
        val showName = text("show_name")
        val libraryId = text("library_id").references(Libraries.libraryId)
        val lastUpdated = long("last_updated")
        val year = integer("year").nullable()
        val premiered = text("premiered").nullable()
        val releaseDate = text("release_date").nullable()
        val folderImageId = text("series_image_id").references(Images.imageId).nullable()
        val actors = text("actors").nullable()
    }

    object Movies : IntIdTable() {
        val movieId = text("movie_id").uniqueIndex()
        val tagline = text("tagline").nullable()
        val country = text("country").nullable()
        val releaseDate = text("release_date").nullable()

        val location = text("location")
        val size = long("size")

        val lastUpdated = long("last_updated")
        val lastModified = long("last_modified")
        val premiered = text("premiered").nullable()
        val libraryId = text("library_id").references(Libraries.libraryId)

        val audioTracks = text("audio_tracks")
        val videoTracks = text("video_tracks")
        val subtitleTracks = text("subtitle_tracks")
        val subtitleFiles = text("subtitle_files")

        val title = text("title")
        val runTime = double("run_time")
        val plot = text("plot").nullable()
        val outline = text("outline").nullable()
        val directors = text("directors").nullable()
        val studios = text("studios").nullable()
        val writers = text("writers").nullable()
        val credits = text("credits").nullable()
        val rating = double("rating").nullable()
        val year = integer("year").nullable()
        val criticRating = integer("critic_rating").nullable()
        val mpaa = text("mpaa").nullable()
        val posterImageId = text("poster_image_id").references(Images.imageId).nullable()
        val backdropImageId = text("backdrop_image_id").references(Images.imageId).nullable()
    }

    object Images : IntIdTable() {
        val imageId = text("image_id").uniqueIndex()
        val libraryId = text("library_id").references(Libraries.libraryId)
        val lastUpdated = long("last_updated")
        val location = text("location")
        val size = long("size")
    }

    object Users : IntIdTable() {
        val username = text("username").uniqueIndex()
        val permissions = text("permissions")
        val preferences = text("preferences")
        val hashedPassword = text("hashed_password")
        val salt = text("salt")
    }

    object Sessions : IntIdTable() {
        val sessionId = text("session_id").uniqueIndex()
        val value = text("value")
    }

    object Libraries : IntIdTable() {
        val libraryId = text("session_id").uniqueIndex()
        val rootPaths = text("root_paths")
        val type = enumeration<LibraryType>("type")
    }

    object Progression : IntIdTable() {
        val username = text("username").references(Users.username)
        val mediaId = text("media_id").index()
        val percent = double("percent").index()
        val timestamp = long("timestamp")
    }

    object LibraryIndices : IntIdTable() {
        val libraryId = text("library_id").uniqueIndex().references(Libraries.libraryId)
        val lastUpdated = long("last_updated")
        val index = blob("index")
    }

    object Schedules : IntIdTable() {
        val scheduleId = text("schedule_id").uniqueIndex()
        val cronExpression = text("cron_expression")
        val scanTasks = text("scan_tasks")
    }

    companion object {
        val log = KotlinLogging.logger { }

        private fun ResultRow.toServerMovieInfo() =
            ServerMovieInfo(
                id = this[Movies.movieId],
                libraryId = this[Movies.libraryId],
                title = this[Movies.title],
                plot = this[Movies.plot],
                outline = this[Movies.outline],
                directors = this[Movies.directors]?.let { JSON.decodeFromString(it) },
                writers = this[Movies.writers]?.let { JSON.decodeFromString(it) },
                credits = this[Movies.credits]?.let { JSON.decodeFromString(it) },
                studios = this[Movies.studios]?.let { JSON.decodeFromString(it) },
                rating = this[Movies.rating],
                criticRating = this[Movies.criticRating],
                mpaa = this[Movies.mpaa],
                premiered = this[Movies.premiered]?.let(LocalDate::parse),
                tagLine = this[Movies.tagline],
                runTime = this[Movies.runTime],
                country = this[Movies.country],
                releaseDate = this[Movies.releaseDate]?.let(LocalDate::parse),
                year = this[Movies.year],
                lastModified = this[Movies.lastModified].let(Instant::fromEpochMilliseconds),
                lastUpdated = this[Movies.lastUpdated].let(Instant::fromEpochMilliseconds),
                backdropImageId = this[Movies.backdropImageId],
                posterImageId = this[Movies.posterImageId],
                fileInfo =
                FileInfo(
                    location = this[Movies.location].let(JSON::decodeFromString),
                    size = this[Movies.size],
                ),
                subtitleFileTracks = this[Movies.subtitleFiles].let(JSON::decodeFromString),
                audioTracks =
                this[Movies.audioTracks].let {
                    JSON.decodeFromString<Map<String, ServerAudioTrack>>(it)
                },
                videoTracks =
                this[Movies.videoTracks].let {
                    JSON.decodeFromString<Map<String, ServerVideoTrack>>(it)
                },
                subtitleTracks =
                this[Movies.subtitleTracks].let {
                    JSON.decodeFromString<Map<String, ServerSubtitleTrack>>(it)
                },
            )

        private fun ResultRow.toProgressedServerMovieInfo() =
            Progressed(
                data = this.toServerMovieInfo(),
                progress = this.toNullableProgress()
            )

        private fun ResultRow.toServerScheduleInfo() =
            ServerScheduleInfo(
                id = this[Schedules.scheduleId],
                cronExpression = this[Schedules.cronExpression],
                scanTasks = JSON.decodeFromString(this[Schedules.scanTasks]),
            )

        private fun ResultRow.toServerImageInfo() =
            ServerImageInfo(
                location = this[Images.location].let { JSON.decodeFromString(it) },
                size = this[Images.size],
                libraryId = this[Images.libraryId],
                lastUpdated = Instant.fromEpochMilliseconds(this[Images.lastUpdated]),
                imageId = this[Images.imageId],
            )

        private fun ResultRow.toServerShowInfo() =
            ServerShowInfo(
                id = this[Shows.showId],
                libraryId = this[Shows.libraryId],
                title = this[Shows.title],
                plot = this[Shows.plot],
                outline = this[Shows.outline],
                originalTitle = this[Shows.originalTitle],
                premiered = this[Shows.premiered]?.let(LocalDate::parse),
                releaseDate = this[Shows.releaseDate]?.let(LocalDate::parse),
                endDate = this[Shows.endDate]?.let(LocalDate::parse),
                mpaa = this[Shows.mpaa],
                imdbId = this[Shows.imdbId],
                tmdbId = this[Shows.tmdbId],
                tvdbId = this[Shows.tvdbId],
                tvRageId = this[Shows.tvRageId],
                rating = this[Shows.rating],
                year = this[Shows.year],
                runTime = this[Shows.runTime],
                aired = this[Shows.aired]?.let(LocalDate::parse),
                genre = this[Shows.genre],
                studio = this[Shows.studio],
                status = this[Shows.status],
                tag = this[Shows.tag]?.let { JSON.decodeFromString(it) },
                actors = this[Shows.actors]?.let { JSON.decodeFromString(it) },
                seriesImageId = this[Shows.seriesImageId],
                backdropImageId = this[Shows.backdropImageId],
                lastUpdated = Instant.fromEpochMilliseconds(this[Shows.lastUpdated]),
            )

        private fun ResultRow.toServerSeasonInfo(): ServerSeasonInfo =
            ServerSeasonInfo(
                seasonInfo =
                SeasonInfo(
                    id = this[Seasons.seasonId],
                    showId = this[Seasons.showId],
                    seasonNumber = this[Seasons.seasonNumber],
                    libraryId = this[Seasons.libraryId],
                    showName = this[Seasons.showName],
                    year = this[Seasons.year],
                    premiered = this[Seasons.premiered]?.let(LocalDate.Companion::parse),
                    releaseDate = this[Seasons.releaseDate]?.let(LocalDate.Companion::parse),
                    folderImageId = this[Seasons.folderImageId],
                    actors = this[Seasons.actors]?.let(JSON::decodeFromString),
                ),
                libraryData =
                LibraryData(
                    libraryId = this[Seasons.libraryId],
                    lastUpdated = Instant.fromEpochMilliseconds(this[Seasons.lastUpdated]),
                ),
            )

        private operator fun <Table : org.jetbrains.exposed.v1.core.Table, T> Alias<Table>?.get(
            col: Column<T>
        ): Column<T> = this?.let { it[col] } ?: col

        private fun ResultRow.toServerEpisodeInfo(episodeTable: Alias<Episodes>? = null): ServerEpisodeInfo =
            ServerEpisodeInfo(
                id = this[episodeTable[Episodes.episodeId]],
                libraryId = this[episodeTable[Episodes.libraryId]],
                showId = this[episodeTable[Episodes.showId]],
                seasonId = this[episodeTable[Episodes.seasonId]],
                title = this[episodeTable[Episodes.title]],
                runTime = this[episodeTable[Episodes.runTime]],
                plot = this[episodeTable[Episodes.plot]],
                outline = this[episodeTable[Episodes.outline]],
                director = this[episodeTable[Episodes.directors]]?.let(JSON::decodeFromString),
                writer = this[episodeTable[Episodes.writers]]?.let(JSON::decodeFromString),
                credits = this[episodeTable[Episodes.credits]]?.let(JSON::decodeFromString),
                rating = this[episodeTable[Episodes.rating]],
                year = this[episodeTable[Episodes.year]],
                episode = this[episodeTable[Episodes.episode]],
                episodeNumberEnd = this[episodeTable[Episodes.episodeNumberEnd]],
                season = this[episodeTable[Episodes.season]],
                showName = this[episodeTable[Episodes.showName]],
                aired = this[episodeTable[Episodes.aired]]?.let(LocalDate::parse),
                episodeImageId = this[episodeTable[Episodes.episodeImageId]],
                lastUpdated = Instant.fromEpochMilliseconds(this[episodeTable[Episodes.lastUpdated]]),
                lastModified = Instant.fromEpochMilliseconds(this[episodeTable[Episodes.lastModified]]),
                fileInfo =
                FileInfo(
                    location = this[episodeTable[Episodes.location]].let(JSON::decodeFromString),
                    size = this[episodeTable[Episodes.size]],
                ),
                subtitleFileTracks = this[episodeTable[Episodes.subtitleFiles]].let(JSON::decodeFromString),
                audioTracks =
                this[episodeTable[Episodes.audioTracks]].let {
                    JSON.decodeFromString<Map<String, ServerAudioTrack>>(it)
                },
                videoTracks =
                this[episodeTable[Episodes.videoTracks]].let {
                    JSON.decodeFromString<Map<String, ServerVideoTrack>>(it)
                },
                subtitleTracks =
                this[episodeTable[Episodes.subtitleTracks]].let {
                    JSON.decodeFromString<Map<String, ServerSubtitleTrack>>(it)
                },
            )

        private fun ResultRow.toProgressedServerEpisodeInfo(
            episodeTable: Alias<Episodes>? = null,
            progressTable: Alias<Progression>? = null
        ) =
            Progressed(
                data = this.toServerEpisodeInfo(episodeTable),
                progress = this.toNullableProgress(progressTable)
            )

        private fun ResultRow.toServerUser() =
            ServerUser(
                user =
                User(
                    username = this[Users.username],
                    permissions = JSON.decodeFromString(this[Users.permissions]),
                    preferences = JSON.decodeFromString(this[Users.preferences]),
                ),
                hashedPass = this[Users.hashedPassword],
                salt = this[Users.salt],
            )

        private fun ResultRow.toLibraryIndex(): LibraryIndex =
            LibraryIndex(
                index = this[LibraryIndices.index].bytes,
                libraryId = this[LibraryIndices.libraryId],
                lastUpdated = Instant.fromEpochMilliseconds(this[LibraryIndices.lastUpdated]),
            )

        private fun ResultRow.toProgress() =
            UserProgress(
                id = this[Progression.mediaId],
                percent = this[Progression.percent],
                timestamp = Instant.fromEpochMilliseconds(this[Progression.timestamp]),
                username = this[Progression.username],
            )

        private fun ResultRow.toNullableProgress(progressTable: Alias<Progression>? = null): UserProgress? {
            val id = this.getOrNull(progressTable[Progression.mediaId])
            val percent = this.getOrNull(progressTable[Progression.percent])
            val timestamp =
                this.getOrNull(progressTable[Progression.timestamp])?.let { Instant.fromEpochMilliseconds(it) }
            val username = this.getOrNull(progressTable[Progression.username])
            return if (id != null && percent != null && timestamp != null && username != null) {
                UserProgress(
                    id = id,
                    percent = percent,
                    timestamp = timestamp,
                    username = username,
                )
            } else {
                null
            }
        }
    }
}
