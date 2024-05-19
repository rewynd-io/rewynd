package io.rewynd.worker.scan.show

import arrow.core.identity
import arrow.core.mapNotNull
import arrow.fx.coroutines.parMapUnordered
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.common.database.Database
import io.rewynd.common.md5
import io.rewynd.common.model.FileInfo
import io.rewynd.common.model.FileLocation
import io.rewynd.common.model.LibraryData
import io.rewynd.common.model.LibraryIndex
import io.rewynd.common.model.ServerEpisodeInfo
import io.rewynd.common.model.ServerImageInfo
import io.rewynd.common.model.ServerSeasonInfo
import io.rewynd.common.model.ServerShowInfo
import io.rewynd.common.model.SubtitleFileTrack
import io.rewynd.model.Library
import io.rewynd.model.SearchResultType
import io.rewynd.model.SeasonInfo
import io.rewynd.worker.ffprobe.FfprobeInfo
import io.rewynd.worker.ffprobe.FfprobeResult
import io.rewynd.worker.scan.Scanner
import io.rewynd.worker.serialize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.ByteBuffersDirectory
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val imageExtensions = setOf("jpg", "jpeg", "png")
private val subtitleExtensions = setOf("srt", "wvtt", "vtt")

class ShowScanner(private val lib: Library, private val db: Database) : Scanner {
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override suspend fun scan() {
        val memoryIndex: ByteBuffersDirectory = ByteBuffersDirectory()
        val analyzer = StandardAnalyzer()
        val start = Clock.System.now()
        lib.rootPaths.forEach { root ->
            File(root).walk().maxDepth(1).filter {
                it.isDirectory &&
                    it.absolutePath != Path(root).absolutePathString() &&
                    !it.name.startsWith(".") &&
                    it.nameWithoutExtension.isNotBlank()
            }.asFlow().parMapUnordered { showFolder ->
                withTimeoutOrNull(10.minutes) {
                    with(scanShow(showFolder, lib)) {
                        if (this.episodes.isNotEmpty()) {
                            images.mapNotNull {
                                try {
                                    db.upsertImage(it)
                                    null // TODO may want to index these at some point
                                } catch (e: Exception) {
                                    log.error(e) { "Failed to upsert image $it" }
                                    null
                                }
                            } +
                                shows.mapNotNull {
                                    try {
                                        db.upsertShow(it)
                                        it.toDocument()
                                    } catch (e: Exception) {
                                        log.error(e) { "Failed to upsert show $it" }
                                        null
                                    }
                                } +
                                seasons.mapNotNull {
                                    try {
                                        db.upsertSeason(it)
                                        it.toDocument()
                                    } catch (e: Exception) {
                                        log.error(e) { "Failed to upsert image $it" }
                                        null
                                    }
                                } +
                                episodes.mapNotNull {
                                    try {
                                        db.upsertEpisode(it)
                                        it.toDocument()
                                    } catch (e: Exception) {
                                        log.error(e) { "Failed to upsert image $it" }
                                        null
                                    }
                                }
                        } else {
                            emptyList<Document>()
                        }
                    }.asFlow()
                } ?: emptyFlow()
            }.flattenMerge().flowOn(Dispatchers.IO).fold(
                IndexWriter(
                    memoryIndex,
                    IndexWriterConfig(analyzer).apply { commitOnClose = true },
                ),
            ) { acc, value ->
                log.info { "Adding $value" }
                acc.addDocument(value)
                acc
            }.close()
        }

        db.upsertLibraryIndex(LibraryIndex(memoryIndex.serialize(), lib.name, start))

        db.cleanEpisodes(start, lib.name)
        db.cleanSeasons(start, lib.name)
        db.cleanShows(start, lib.name)
        db.cleanImages(start, lib.name)
    }

    private suspend fun scanShow(
        showDir: File,
        lib: Library,
    ): ShowScanResults {
        val showId = showDir.id(lib)
        val nfo =
            Path(showDir.absolutePath, "tvshow.nfo").let {
                if (it.exists()) it else null
            }?.readText()
                ?.parseShowNfo()
        log.info { nfo }
        val folderImage = showDir.findFolderImage()
        val backdropImage = showDir.findBackdropImage()

        val showInfo =
            ServerShowInfo(
                id = showId,
                libraryId = lib.name,
                title = nfo?.title ?: showDir.nameWithoutExtension,
                plot = nfo?.plot,
                outline = nfo?.outline,
                originalTitle = nfo?.originaltitle,
                premiered = nfo?.premiered,
                releaseDate = nfo?.releasedate,
                endDate = nfo?.enddate,
                mpaa = nfo?.mpaa,
                imdbId = nfo?.imdb_id,
                tmdbId = nfo?.tmdbid,
                tvdbId = nfo?.tvdbid,
                tvRageId = nfo?.tvrageid,
                rating = nfo?.rating?.toDouble(),
                year = nfo?.year?.toDouble(),
                runTime = nfo?.runTime?.toDouble(),
                genre = nfo?.genre,
                studio = nfo?.studio,
                status = nfo?.status,
                tag = nfo?.tag,
                actors = nfo?.actor,
                seriesImageId = folderImage?.imageId,
                backdropImageId = backdropImage?.imageId,
                lastUpdated = Clock.System.now(),
            )

        return showDir.walk().maxDepth(1).filter {
            it.isDirectory &&
                it.absolutePath != showDir.absolutePath &&
                !it.name.startsWith(".") &&
                it.nameWithoutExtension.isNotBlank()
        }.fold(ShowScanResults.EMPTY) { acc, file ->
            acc + scanSeason(file, showInfo)
        } +
            ShowScanResults(
                shows = setOf(showInfo),
                images = setOfNotNull(folderImage, backdropImage),
            ).also {
                log.info { "Processed ${showDir.absolutePath}" }
            }
    }

    private suspend fun scanSeason(
        seasonDir: File,
        showInfo: ServerShowInfo,
    ): ShowScanResults {
        val seasonId = seasonDir.id(lib)
        val nfo =
            Path(seasonDir.absolutePath, "season.nfo").let {
                log.info { it }
                if (it.exists()) it else null
            }?.readText()?.parseSeasonNfo()
        log.info { nfo }
        val folderImage = seasonDir.findFolderImage()
        val seasonInfo =
            ServerSeasonInfo(
                seasonInfo =
                SeasonInfo(
                    id = seasonId,
                    showId = showInfo.id,
                    seasonNumber = (nfo?.seasonnumber ?: seasonDir.name.parseSeasonNumber() ?: 0).toDouble(),
                    libraryId = lib.name,
                    showName = showInfo.title,
                    year = nfo?.year?.toDouble(),
                    premiered = nfo?.premiered,
                    releaseDate = nfo?.releasedate,
                    folderImageId = folderImage?.imageId,
                    actors = listOf(),
                ),
                libraryData =
                LibraryData(
                    libraryId = lib.name,
                ),
            )

        return (
            seasonDir.walk().filter {
                it.isFile && !it.name.startsWith(".") && it.nameWithoutExtension.isNotBlank() &&
                    !imageExtensions.contains(
                        it.extension,
                    ) &&
                    !subtitleExtensions.contains(it.extension) && it.extension != "nfo"
            }
                .fold(ShowScanResults.EMPTY) { acc, file ->

                    acc + scanEpisode(file, seasonInfo, showInfo)
                } +
                ShowScanResults(
                    images = setOfNotNull(folderImage),
                    seasons =
                    setOf(
                        seasonInfo,
                    ),
                )
            ).let { if (it.episodes.isNotEmpty()) it else ShowScanResults.EMPTY }.also {
            log.info { "Processed ${seasonDir.absolutePath}" }
        }
    }

    private suspend fun scanEpisode(
        episodeFile: File,
        seasonInfo: ServerSeasonInfo,
        showInfo: ServerShowInfo,
    ): ShowScanResults =
        try {
            val episodeId = episodeFile.id(lib)
            val curr = db.getEpisode(episodeId)
            val nfo =
                Path(episodeFile.parent, "${episodeFile.nameWithoutExtension}.nfo").let {
                    log.info { it }
                    if (it.exists()) it else null
                }?.readText()?.parseEpsiodeNfo()
            val subtitleFiles =
                episodeFile.parentFile.walk().maxDepth(1).filter {
                    it.name.startsWith(episodeFile.nameWithoutExtension) &&
                        subtitleExtensions.contains(
                            it.extension,
                        )
                }.associate { it.nameWithoutExtension to FileLocation.LocalFile(it.absolutePath) }
            val subtitleFileTracks =
                subtitleFiles.mapValues { entry ->
                    FfprobeResult.parseFile(Path(entry.value.path).toFile())
                        .extractInfo().subtitleTracks.values.firstOrNull()
                        ?.let {
                            SubtitleFileTrack(entry.value, it)
                        }
                }.mapNotNull { it.value }
            val episodeImageFile = episodeFile.findEpisodeImage()
            val ffprobe =
                curr?.let {
                    if (episodeFile.lastModified() < it.lastUpdated.toEpochMilliseconds()) {
                        it.toFfprobeInfo()
                    } else {
                        null
                    }
                } ?: withTimeoutOrNull(5.seconds) {
                    log.info { "Probing ${episodeFile.absolutePath}" }
                    FfprobeResult.parseFile(episodeFile).extractInfo().also {
                        log.info { "Done Probing ${episodeFile.absolutePath}" }
                    }
                }
            if (ffprobe != null && ffprobe.videoTracks.isNotEmpty() && ffprobe.runTime > 1.0) {
                ShowScanResults(
                    images = setOfNotNull(episodeImageFile),
                    episodes =
                    setOf(
                        ServerEpisodeInfo(
                            id = episodeId,
                            libraryId = this.lib.name,
                            audioTracks = ffprobe.audioTracks,
                            videoTracks = ffprobe.videoTracks,
                            subtitleTracks = ffprobe.subtitleTracks,
                            showId = showInfo.id,
                            seasonId = seasonInfo.seasonInfo.id,
                            title = nfo?.title ?: episodeFile.nameWithoutExtension,
                            runTime = ffprobe.runTime,
                            plot = nfo?.plot,
                            outline = nfo?.outline,
                            director = nfo?.director,
                            writer = nfo?.writer,
                            credits = nfo?.credits,
                            rating = nfo?.rating,
                            year = nfo?.year?.toDouble(),
                            episode = nfo?.episode?.toDouble(),
                            episodeNumberEnd = nfo?.episodenumberend?.toDouble(),
                            season = seasonInfo.seasonInfo.seasonNumber,
                            showName = showInfo.title,
                            // TODO nfo?.aired is and should be a string, not a double
                            aired = null,
                            episodeImageId = episodeImageFile?.imageId,
                            fileInfo =
                            FileInfo(
                                location = FileLocation.LocalFile(episodeFile.absolutePath),
                                size = Files.size(episodeFile.toPath()),
                            ),
                            subtitleFileTracks = subtitleFileTracks,
                            lastUpdated = Clock.System.now(),
                        ),
                    ),
                )
            } else {
                ShowScanResults.EMPTY
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to process $episodeFile" }
            ShowScanResults.EMPTY
        }.also {
            log.info { "Processed ${episodeFile.absolutePath}" }
        }

    private val xmlMapper =
        XmlMapper().apply {
            registerModule(
                KotlinModule.Builder().build(),
            )
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    private fun String.parseSeasonNfo(): SeasonNfo? =
        try {
            xmlMapper.readValue(this.trim())
        } catch (e: Exception) {
            log.error(e) { "Failed to parse XML: $this" }
            null
        }

    private fun String.parseEpsiodeNfo(): EpisodeNfo? =
        try {
            xmlMapper.readValue(this.trim())
        } catch (e: Exception) {
            log.error(e) { "Failed to parse XML: $this" }
            null
        }

    private fun String.parseShowNfo(): ShowNfo? =
        try {
            xmlMapper.readValue(this.trim())
        } catch (e: Exception) {
            log.error(e) { "Failed to parse XML: $this" }
            null
        }

    private fun File.findEpisodeImage(): ServerImageInfo? =
        (
            Path(this.parent).toFile().walk().maxDepth(2).filter {
                it.name.startsWith(this.nameWithoutExtension) &&
                    imageExtensions.contains(
                        it.extension,
                    )
            }.firstOrNull()
            )?.let {
            ServerImageInfo(
                location = FileLocation.LocalFile(it.absolutePath),
                size = 0L,
                libraryId = this@ShowScanner.lib.name,
                imageId = it.id(lib),
                lastUpdated = Clock.System.now(),
            )
        }

    private fun File.findFolderImage(): ServerImageInfo? =
        walk().maxDepth(1).filter {
            it.nameWithoutExtension == "folder" && imageExtensions.contains(it.extension)
        }.firstOrNull()?.let {
            ServerImageInfo(
                location = FileLocation.LocalFile(it.absolutePath),
                size = 0L,
                libraryId = this@ShowScanner.lib.name,
                imageId = it.id(lib),
                lastUpdated = Clock.System.now(),
            )
        }

    private fun File.findBackdropImage(): ServerImageInfo? =
        walk().maxDepth(1).filter {
            listOf("backdrop", "banner").contains(it.nameWithoutExtension) &&
                imageExtensions.contains(
                    it.extension,
                )
        }.firstOrNull()?.let {
            ServerImageInfo(
                location = FileLocation.LocalFile(it.absolutePath),
                size = 0L,
                libraryId = this@ShowScanner.lib.name,
                imageId = it.id(lib),
                lastUpdated = Clock.System.now(),
            )
        }

    companion object {
        private val log by lazy { KotlinLogging.logger { } }
    }
}

private fun File.id(lib: Library) = md5("${lib.name}:${this.absolutePath}")

private fun String.parseSeasonNumber(): Int? = split(" ").lastOrNull()?.toIntOrNull()

private fun ServerEpisodeInfo.toFfprobeInfo(): FfprobeInfo =
    FfprobeInfo(
        this.audioTracks,
        this.videoTracks,
        this.subtitleTracks,
        this.runTime,
    )

private fun ServerSeasonInfo.toDocument() =
    Document().apply {
        identity(this@toDocument.seasonInfo.id)
        add(StringField("title", this@toDocument.seasonInfo.formatTitle(), Field.Store.YES))
        add(StoredField("id", this@toDocument.seasonInfo.id))
        add(StoredField("description", this@toDocument.seasonInfo.formatTitle()))
        add(StoredField("type", SearchResultType.Season.name))
    }

private fun SeasonInfo.formatTitle() = "$showName - Season ${seasonNumber.roundToInt()}"

private fun ServerEpisodeInfo.toDocument() =
    Document().apply {
        identity(this@toDocument.id)
        add(StringField("title", this@toDocument.formatTitle(), Field.Store.YES))
        add(StoredField("id", this@toDocument.id))
        add(StoredField("description", this@toDocument.plot ?: this@toDocument.outline ?: ""))
        add(StoredField("type", SearchResultType.Episode.name))
    }

private fun ServerShowInfo.toDocument() =
    Document().apply {
        identity(this@toDocument.id)
        add(StringField("title", this@toDocument.title, Field.Store.YES))
        add(StoredField("id", this@toDocument.id))
        add(StoredField("description", this@toDocument.plot ?: this@toDocument.outline ?: ""))
        add(StoredField("type", SearchResultType.Show.name))
    }

private fun ServerEpisodeInfo.formatTitle() =
    "$showName - S${"%02d".format(season?.roundToInt() ?: 0)}E${
        "%02d".format(
            episode?.roundToInt() ?: 0,
        )
    }${
        episodeNumberEnd?.let {
            "-%02d".format(it.roundToInt())
        } ?: ""
    } - $title"
