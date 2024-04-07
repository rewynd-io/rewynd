package io.rewynd.common.database

import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.common.config.DatabaseConfig
import io.rewynd.common.config.datasource
import io.rewynd.common.database.PostgresExtensions.upsert
import io.rewynd.common.generateSalt
import io.rewynd.common.hashPassword
import io.rewynd.common.model.FileInfo
import io.rewynd.common.model.LibraryData
import io.rewynd.common.model.LibraryIndex
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
import io.rewynd.model.Library
import io.rewynd.model.LibraryType
import io.rewynd.model.ListEpisodesByLastUpdatedOrder
import io.rewynd.model.SeasonInfo
import io.rewynd.model.User
import io.rewynd.model.UserPermissions
import io.rewynd.model.UserPreferences
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import kotlin.math.roundToInt
import org.jetbrains.exposed.sql.Database as Connection

class PostgresDatabase(
    config: DatabaseConfig.PostgresConfig,
    private val conn: Connection = Connection.connect(config.datasource),
) : Database {
    override suspend fun init() {
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Sessions,
                Libraries,
                Shows,
                Seasons,
                Episodes,
                Progression,
                LibraryIndicies,
                Schedules,
            )

            if (Users.selectAll().firstOrNull() == null) {
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
    }

    override suspend fun getUser(username: String): ServerUser? =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Users.selectAll().where { Users.username eq username }.limit(1).firstOrNull()?.toServerUser()
        }

    override suspend fun upsertUser(user: ServerUser): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Users.upsert(Users.username) {
                it[Users.username] = user.user.username
                it[Users.hashedPassword] = user.hashedPass
                it[Users.preferences] = Json.encodeToString(user.user.preferences)
                it[Users.permissions] = Json.encodeToString(user.user.permissions)
                it[Users.salt] = user.salt
            }.insertedCount == 1
        }

    override suspend fun deleteUser(username: String): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Users.deleteWhere {
                Users.username eq username
            } == 1
        }

    override suspend fun listUsers(cursor: String?): List<ServerUser> =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Users.selectAll().let {
                if (cursor != null) {
                    it.where { Users.username greater cursor }
                } else {
                    it
                }
            }.orderBy(Users.username, SortOrder.ASC).map {
                it.toServerUser()
            }
        }

    override suspend fun getLibrary(libraryId: String): Library? =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Libraries.selectAll().where { Libraries.libraryId eq libraryId }.firstOrNull()?.let {
                Library(
                    name = it[Libraries.libraryId],
                    type = it[Libraries.type],
                    rootPaths = Json.decodeFromString<List<String>>(it[Libraries.rootPaths]),
                )
            }
        }

    override suspend fun upsertLibrary(lib: Library): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Libraries.upsert(Libraries.libraryId) {
                it[Libraries.libraryId] = lib.name
                it[Libraries.type] = lib.type
                it[Libraries.rootPaths] = Json.encodeToString(lib.rootPaths)
            }.insertedCount == 1
        }

    override suspend fun deleteLibrary(libraryId: String): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Libraries.deleteWhere {
                Libraries.libraryId eq libraryId
            } == 1
        }

    override suspend fun listLibraries(cursor: String?): List<Library> =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Libraries.selectAll().let {
                if (cursor != null) {
                    it.where { Libraries.libraryId greater cursor }
                } else {
                    it
                }
            }.orderBy(Libraries.libraryId, SortOrder.ASC).map {
                Library(
                    name = it[Libraries.libraryId],
                    type = it[Libraries.type],
                    rootPaths = Json.decodeFromString<List<String>>(it[Libraries.rootPaths]),
                )
            }
        }

    override suspend fun getShow(showId: String): ServerShowInfo? =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Shows.selectAll().where { Shows.showId eq showId }.firstOrNull()?.toServerShowInfo()
        }

    override suspend fun upsertShow(show: ServerShowInfo): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Shows.upsert(Shows.showId) {
                it[Shows.showId] = show.id
                it[Shows.libraryId] = show.libraryId
                it[Shows.title] = show.title
                it[Shows.plot] = show.plot
                it[Shows.outline] = show.outline
                it[Shows.originalTitle] = show.originalTitle
                it[Shows.premiered] = show.premiered
                it[Shows.releaseDate] = show.releaseDate
                it[Shows.endDate] = show.endDate
                it[Shows.mpaa] = show.mpaa
                it[Shows.imdbId] = show.imdbId
                it[Shows.tmdbId] = show.tmdbId
                it[Shows.tvdbId] = show.tvdbId
                it[Shows.tvRageId] = show.tvRageId
                it[Shows.rating] = show.rating
                it[Shows.year] = show.year?.roundToInt()
                it[Shows.runTime] = show.runTime
                it[Shows.aired] = show.aired
                it[Shows.genre] = show.genre
                it[Shows.studio] = show.studio
                it[Shows.status] = show.status
                it[Shows.tag] = show.tag?.let(Json.Default::encodeToString)
                it[Shows.actors] = show.actors?.let(Json.Default::encodeToString)
                it[Shows.seriesImageId] = show.seriesImageId
                it[Shows.backdropImageId] = show.backdropImageId
                it[Shows.lastUpdated] = show.lastUpdated.toEpochMilliseconds()
            }.insertedCount == 1
        }

    override suspend fun deleteShow(showId: String): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Shows.deleteWhere { Shows.showId eq showId } == 1
        }

    override suspend fun listShows(
        libraryId: String,
        cursor: String?,
    ): List<ServerShowInfo> =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Shows.selectAll().where {
                if (cursor == null) {
                    Shows.libraryId eq libraryId
                } else {
                    (Shows.libraryId eq libraryId) and (Shows.showId greater cursor)
                }
            }.map { it.toServerShowInfo() }
        }

    override suspend fun getSeason(seasonId: String): ServerSeasonInfo? =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Seasons.selectAll().where { Seasons.seasonId eq seasonId }.firstOrNull()?.toServerSeasonInfo()
        }

    override suspend fun upsertSeason(season: ServerSeasonInfo): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Seasons.upsert(Seasons.seasonId) {
                it[Seasons.seasonId] = season.seasonInfo.id
                it[Seasons.showId] = season.seasonInfo.showId
                it[Seasons.seasonNumber] = season.seasonInfo.seasonNumber.roundToInt()
                it[Seasons.libraryId] = season.libraryData.libraryId
                it[Seasons.showName] = season.seasonInfo.showName ?: "" // TODO should not be nullable in spec
                it[Seasons.year] = season.seasonInfo.year?.roundToInt()
                it[Seasons.premiered] = season.seasonInfo.premiered
                it[Seasons.releaseDate] = season.seasonInfo.releaseDate
                it[Seasons.folderImageId] = season.seasonInfo.folderImageId
                it[Seasons.actors] = season.seasonInfo.actors?.let(Json.Default::encodeToString)
                it[Seasons.libraryId] = season.libraryData.libraryId
                it[Seasons.lastUpdated] = season.libraryData.lastUpdated.toEpochMilliseconds()
            }.insertedCount == 1
        }

    override suspend fun deleteSeason(seasonId: String): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Seasons.deleteWhere { Seasons.seasonId eq seasonId } == 1
        }

    override suspend fun listSeasons(showId: String): List<ServerSeasonInfo> =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Seasons.selectAll().where { Seasons.showId eq showId }.map { it.toServerSeasonInfo() }
        }

    override suspend fun getEpisode(episodeId: String): ServerEpisodeInfo? =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Episodes.selectAll().where { Episodes.episodeId eq episodeId }.firstOrNull()?.toServerEpisodeInfo()
        }

    override suspend fun upsertEpisode(episode: ServerEpisodeInfo): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Episodes.upsert(Episodes.episodeId) {
                it[Episodes.showId] = episode.showId
                it[Episodes.showName] = episode.showName ?: "" // TODO should not be nullable in spec
                it[Episodes.seasonId] = episode.seasonId
                it[Episodes.season] = episode.season?.roundToInt() ?: 0 // TODO should not be nullable in spec
                it[Episodes.episodeId] = episode.id
                it[Episodes.location] = episode.fileInfo.location.let(Json.Default::encodeToString)
                it[Episodes.size] = episode.fileInfo.size
                it[Episodes.lastUpdated] = episode.lastUpdated.toEpochMilliseconds()
                it[Episodes.libraryId] = episode.libraryId
                it[Episodes.audioTracks] = episode.audioTracks.let(Json.Default::encodeToString)
                it[Episodes.videoTracks] = episode.videoTracks.let(Json.Default::encodeToString)
                it[Episodes.subtitleTracks] = episode.subtitleTracks.let(Json.Default::encodeToString)
                it[Episodes.subtitleFiles] = episode.subtitleFileTracks.let(Json.Default::encodeToString)
                it[Episodes.title] = episode.title
                it[Episodes.runTime] = episode.runTime
                it[Episodes.plot] = episode.plot
                it[Episodes.outline] = episode.outline
                it[Episodes.directors] = episode.director?.let(Json.Default::encodeToString)
                it[Episodes.writers] = episode.writer?.let(Json.Default::encodeToString)
                it[Episodes.credits] = episode.credits?.let(Json.Default::encodeToString)
                it[Episodes.rating] = episode.rating
                it[Episodes.year] = episode.year?.roundToInt()
                it[Episodes.episode] =
                    episode.episode?.roundToInt() ?: 0 // TODO should not be nullable in spec
                it[Episodes.episodeNumberEnd] = episode.episodeNumberEnd?.roundToInt()
                it[Episodes.season] = episode.season?.roundToInt() ?: 0 // TODO should not be nullable in spec
                it[Episodes.aired] = episode.aired
                it[Episodes.episodeImageId] = episode.episodeImageId
            }.insertedCount == 1
        }

    override suspend fun deleteEpisode(episodeId: String): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Episodes.deleteWhere {
                Episodes.episodeId eq episodeId
            } == 1
        }

    override suspend fun listEpisodes(
        seasonId: String,
        cursor: String?,
    ): List<ServerEpisodeInfo> =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Episodes.selectAll()
                .where {
                    cursor?.let {
                        Episodes.seasonId eq seasonId and (Episodes.episodeId greater it)
                    } ?: (Episodes.seasonId eq seasonId)
                }
                .orderBy(Episodes.episodeId, SortOrder.ASC)
                .limit(100)
                .map { it.toServerEpisodeInfo() }
        }

    override suspend fun listEpisodesByLastUpdated(
        cursor: Long?,
        order: ListEpisodesByLastUpdatedOrder,
    ): List<ServerEpisodeInfo> =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            if (cursor != null) {
                Episodes.selectAll().where {
                    when (order) {
                        ListEpisodesByLastUpdatedOrder.Newest -> Episodes.lastUpdated.less(cursor)
                        ListEpisodesByLastUpdatedOrder.Oldest -> Episodes.lastUpdated.greater(cursor)
                    }
                }
            } else {
                Episodes.selectAll()
            }.orderBy(
                Episodes.lastUpdated,
                when (order) {
                    ListEpisodesByLastUpdatedOrder.Newest -> SortOrder.DESC
                    ListEpisodesByLastUpdatedOrder.Oldest -> SortOrder.ASC
                },
            ).limit(100).take(100).map { it.toServerEpisodeInfo() }
        }

    override suspend fun getMovie(movieId: String): ServerMovieInfo? {
        TODO("Not yet implemented")
    }

    override suspend fun upsertMovie(movieInfo: ServerMovieInfo): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMovie(movieId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun listMovies(libraryId: String): List<ServerMovieInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getSchedule(scheduleId: String): ServerScheduleInfo? =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Schedules.selectAll().where { Schedules.scheduleId eq scheduleId }.firstOrNull()
                ?.toServerScheduleInfo()
        }

    override suspend fun upsertSchedule(schedule: ServerScheduleInfo): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Schedules.upsert(Schedules.scheduleId) {
                it[Schedules.scheduleId] = schedule.id
                it[Schedules.cronExpression] = schedule.cronExpression
                it[Schedules.scanTasks] = Json.encodeToString(schedule.scanTasks)
            }.insertedCount == 1
        }

    override suspend fun deleteSchedule(scheduleId: String): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Schedules.deleteWhere {
                Schedules.scheduleId eq scheduleId
            } == 1
        }

    override suspend fun listSchedules(cursor: String?): List<ServerScheduleInfo> =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Schedules.selectAll().let {
                if (cursor != null) {
                    it.where { Schedules.scheduleId greater cursor }
                } else {
                    it
                }
            }.orderBy(Schedules.scheduleId, SortOrder.ASC).map {
                ServerScheduleInfo(
                    id = it[Schedules.scheduleId],
                    cronExpression = it[Schedules.cronExpression],
                    scanTasks = Json.decodeFromString(it[Schedules.scanTasks]),
                )
            }
        }

    override suspend fun getImage(imageId: String): ServerImageInfo? =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Images.selectAll().where { Images.imageId eq imageId }.firstOrNull()?.toServerImageInfo()
        }

    override suspend fun upsertImage(imageInfo: ServerImageInfo): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Images.upsert(Images.imageId) {
                it[Images.imageId] = imageInfo.imageId
                it[Images.size] = imageInfo.size
                it[Images.lastUpdated] = imageInfo.lastUpdated.toEpochMilliseconds()
                it[Images.libraryId] = imageInfo.libraryId
                it[Images.location] = imageInfo.location.let(Json.Default::encodeToString)
            }.insertedCount == 1
        }

    override suspend fun deleteImage(imageId: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun listImages(libraryId: String): List<ServerImageInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun mkSessionStorage(): SessionStorage {
        return object : SessionStorage {
            override suspend fun invalidate(id: String) =
                newSuspendedTransaction(currentCoroutineContext(), conn) {
                    Sessions.deleteWhere {
                        Sessions.sessionId eq id
                    }
                    Unit
                }

            override suspend fun write(
                id: String,
                value: String,
            ) = newSuspendedTransaction(currentCoroutineContext(), conn) {
                Sessions.upsert(Sessions.sessionId) {
                    it[Sessions.sessionId] = id
                    it[Sessions.value] = value
                }
                Unit
            }

            override suspend fun read(id: String): String =
                newSuspendedTransaction(currentCoroutineContext(), conn) {
                    Sessions.selectAll().where { Sessions.sessionId eq id }.firstOrNull()?.getOrNull(Sessions.value)
                        ?: throw NoSuchElementException("Session $id not found")
                }
        }
    }

    override suspend fun cleanShows(
        start: Instant,
        libraryId: String,
    ) = newSuspendedTransaction(currentCoroutineContext(), conn) {
        Shows.deleteWhere {
            Shows.lastUpdated less start.toEpochMilliseconds() and (
                Shows.libraryId eq libraryId
            )
        }
    }

    override suspend fun cleanSeasons(
        start: Instant,
        libraryId: String,
    ) = newSuspendedTransaction(currentCoroutineContext(), conn) {
        Seasons.deleteWhere { Seasons.lastUpdated less start.toEpochMilliseconds() and (Seasons.libraryId eq libraryId) }
    }

    override suspend fun cleanEpisodes(
        start: Instant,
        libraryId: String,
    ) = newSuspendedTransaction(currentCoroutineContext(), conn) {
        Episodes.deleteWhere {
            Episodes.lastUpdated less start.toEpochMilliseconds() and (Episodes.libraryId eq libraryId)
        }
    }

    override suspend fun cleanImages(
        start: Instant,
        libraryId: String,
    ) = newSuspendedTransaction(currentCoroutineContext(), conn) {
        Images.deleteWhere { Images.lastUpdated less start.toEpochMilliseconds() and (Images.libraryId eq libraryId) }
    }

    override suspend fun getLibraryIndex(
        libraryId: String,
        updatedAfter: Instant?,
    ): LibraryIndex? =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            LibraryIndicies.selectAll().where {
                if (updatedAfter != null) {
                    LibraryIndicies.libraryId eq libraryId and (LibraryIndicies.lastUpdated greater updatedAfter.toEpochMilliseconds())
                } else {
                    LibraryIndicies.libraryId eq libraryId
                }
            }.firstOrNull()
        }?.toLibraryIndex()

    override suspend fun upsertLibraryIndex(index: LibraryIndex): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            LibraryIndicies.upsert(LibraryIndicies.libraryId) {
                it[LibraryIndicies.libraryId] = index.libraryId
                it[LibraryIndicies.lastUpdated] = index.lastUpdated.toEpochMilliseconds()
                it[LibraryIndicies.index] = ExposedBlob(index.index)
            }
        }.insertedCount == 1

    override suspend fun deleteLibraryIndex(libraryId: String): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            LibraryIndicies.deleteWhere { LibraryIndicies.libraryId eq libraryId }
        } == 1

    override suspend fun listLibraryIndexes(): List<LibraryIndex> {
        TODO("Not yet implemented")
    }

    override suspend fun getProgress(
        id: String,
        username: String,
    ): UserProgress? =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Progression.selectAll().where { Progression.mediaId eq id }.firstOrNull()?.toProgress()
        }

    override suspend fun upsertProgress(progress: UserProgress): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Progression.upsert(Progression.mediaId) {
                it[Progression.mediaId] = progress.id
                it[Progression.timestamp] = progress.timestamp.toEpochMilliseconds()
                it[Progression.username] = progress.username
                it[Progression.percent] = progress.percent
            }.insertedCount == 1
        }

    override suspend fun deleteProgress(
        id: String,
        username: String,
    ): Boolean =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Progression.deleteWhere {
                Progression.mediaId eq id
            } == 1
        }

    override suspend fun listRecentProgress(
        username: String,
        cursor: Instant?,
        minPercent: Double,
        maxPercent: Double,
    ): List<UserProgress> =
        newSuspendedTransaction(currentCoroutineContext(), conn) {
            Progression.selectAll().where {
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
            }.orderBy(Progression.timestamp to SortOrder.ASC, Progression.mediaId to SortOrder.ASC)
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
        val aired = double("aired").nullable()
        val genre = text("genre").nullable()
        val studio = text("studio").nullable()
        val status = text("status").nullable()
        val tag = text("tag").nullable()
        val actors = text("actors").nullable()
        val seriesImageId = text("series_image_id").references(Images.imageId).nullable()
        val backdropImageId = text("backdrop_image_id").references(Images.imageId).nullable()
    }

    object Episodes : IntIdTable() {
        val showId = text("show_id").references(Shows.showId)
        val showName = text("show_name")
        val seasonId = text("season_id").references(Seasons.seasonId)
        val episodeId = text("episode_id").uniqueIndex()

        val location = text("location")
        val size = long("size")

        val lastUpdated = long("lastUpdated")
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
        val episode = integer("episode")
        val episodeNumberEnd = integer("premiered").nullable()
        val season = integer("season")
        val aired = double("aired").nullable()
        val episodeImageId = text("episode_image_id").references(Images.imageId).nullable()
    }

    object Seasons : IntIdTable() {
        val seasonId = text("season_id").uniqueIndex()
        val seasonNumber = integer("season_number")
        val showId = text("show_id").references(Shows.showId)
        val showName = text("show_name")
        val libraryId = text("library_id").references(Libraries.libraryId)
        val lastUpdated = long("lastUpdated")
        val year = integer("year").nullable()
        val premiered = text("premiered").nullable()
        val releaseDate = text("release_date").nullable()
        val folderImageId = text("series_image_id").references(Images.imageId).nullable()
        val actors = text("actors").nullable()
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
        val mediaId = text("media_id").uniqueIndex()
        val percent = double("percent")
        val timestamp = long("timestamp")
    }

    object LibraryIndicies : IntIdTable() {
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

        private fun ResultRow.toServerScheduleInfo() =
            ServerScheduleInfo(
                id = this[Schedules.scheduleId],
                cronExpression = this[Schedules.cronExpression],
                scanTasks = Json.decodeFromString(this[Schedules.scanTasks]),
            )

        private fun ResultRow.toServerImageInfo() =
            ServerImageInfo(
                location = this[Images.location].let { Json.decodeFromString(it) },
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
                premiered = this[Shows.premiered],
                releaseDate = this[Shows.releaseDate],
                endDate = this[Shows.endDate],
                mpaa = this[Shows.mpaa],
                imdbId = this[Shows.imdbId],
                tmdbId = this[Shows.tmdbId],
                tvdbId = this[Shows.tvdbId],
                tvRageId = this[Shows.tvRageId],
                rating = this[Shows.rating],
                year = this[Shows.year]?.toDouble(),
                runTime = this[Shows.runTime],
                aired = this[Shows.aired]?.toDouble(),
                genre = this[Shows.genre],
                studio = this[Shows.studio],
                status = this[Shows.status],
                tag = this[Shows.tag]?.let { Json.decodeFromString(it) },
                actors = this[Shows.actors]?.let { Json.decodeFromString(it) },
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
                        seasonNumber = this[Seasons.seasonNumber].toDouble(),
                        libraryId = this[Seasons.libraryId],
                        showName = this[Seasons.showName],
                        year = this[Seasons.year]?.toDouble(),
                        premiered = this[Seasons.premiered],
                        releaseDate = this[Seasons.releaseDate],
                        folderImageId = this[Seasons.folderImageId],
                        actors = this[Seasons.actors]?.let(Json.Default::decodeFromString),
                    ),
                libraryData =
                    LibraryData(
                        libraryId = this[Seasons.libraryId],
                        lastUpdated = Instant.fromEpochMilliseconds(this[Seasons.lastUpdated]),
                    ),
            )

        private fun ResultRow.toServerEpisodeInfo(): ServerEpisodeInfo =
            ServerEpisodeInfo(
                id = this[Episodes.episodeId],
                libraryId = this[Episodes.libraryId],
                showId = this[Episodes.showId],
                seasonId = this[Episodes.seasonId],
                title = this[Episodes.title],
                runTime = this[Episodes.runTime],
                plot = this[Episodes.plot],
                outline = this[Episodes.outline],
                director = this[Episodes.directors]?.let(Json.Default::decodeFromString),
                writer = this[Episodes.writers]?.let(Json.Default::decodeFromString),
                credits = this[Episodes.credits]?.let(Json.Default::decodeFromString),
                rating = this[Episodes.rating],
                year = this[Episodes.year]?.toDouble(),
                episode = this[Episodes.episode].toDouble(),
                episodeNumberEnd = this[Episodes.episodeNumberEnd]?.toDouble(),
                season = this[Episodes.season].toDouble(),
                showName = this[Episodes.showName],
                aired = this[Episodes.aired]?.toDouble(),
                episodeImageId = this[Episodes.episodeImageId],
                lastUpdated = Instant.fromEpochMilliseconds(this[Episodes.lastUpdated]),
                fileInfo =
                    FileInfo(
                        location = this[Episodes.location].let(Json.Default::decodeFromString),
                        size = this[Episodes.size],
                    ),
                subtitleFileTracks = this[Episodes.subtitleFiles].let(Json.Default::decodeFromString),
                audioTracks =
                    this[Episodes.audioTracks].let {
                        Json.decodeFromString<Map<String, ServerAudioTrack>>(it)
                    },
                videoTracks =
                    this[Episodes.videoTracks].let {
                        Json.decodeFromString<Map<String, ServerVideoTrack>>(it)
                    },
                subtitleTracks =
                    this[Episodes.subtitleTracks].let {
                        Json.decodeFromString<Map<String, ServerSubtitleTrack>>(it)
                    },
            )

        private fun ResultRow.toServerUser() =
            ServerUser(
                user =
                    User(
                        username = this[Users.username],
                        permissions = Json.decodeFromString(this[Users.permissions]),
                        preferences = Json.decodeFromString(this[Users.preferences]),
                    ),
                hashedPass = this[Users.hashedPassword],
                salt = this[Users.salt],
            )

        private fun ResultRow.toLibraryIndex(): LibraryIndex =
            LibraryIndex(
                index = this[LibraryIndicies.index].bytes,
                libraryId = this[LibraryIndicies.libraryId],
                lastUpdated = Instant.fromEpochMilliseconds(this[LibraryIndicies.lastUpdated]),
            )

        private fun ResultRow.toProgress() =
            UserProgress(
                id = this[Progression.mediaId],
                percent = this[Progression.percent],
                timestamp = Instant.fromEpochMilliseconds(this[Progression.timestamp]),
                username = this[Progression.username],
            )
    }
}
