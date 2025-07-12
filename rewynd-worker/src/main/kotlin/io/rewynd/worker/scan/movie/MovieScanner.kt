package io.rewynd.worker.scan.movie

import arrow.core.identity
import arrow.core.mapValuesNotNull
import arrow.fx.coroutines.parMapUnordered
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.common.database.Database
import io.rewynd.common.model.FileInfo
import io.rewynd.common.model.FileLocation
import io.rewynd.common.model.LibraryIndex
import io.rewynd.common.model.ServerMovieInfo
import io.rewynd.common.model.SubtitleFileTrack
import io.rewynd.model.Library
import io.rewynd.model.SearchResultType
import io.rewynd.worker.ffprobe.FfprobeInfo
import io.rewynd.worker.ffprobe.FfprobeResult
import io.rewynd.worker.scan.Scanner
import io.rewynd.worker.scan.findMediaImage
import io.rewynd.worker.scan.id
import io.rewynd.worker.scan.isImageFile
import io.rewynd.worker.scan.isNfoFile
import io.rewynd.worker.scan.isSubtitleFile
import io.rewynd.worker.serialize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.kensand.kielbasa.coroutines.coRunCatching
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
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class MovieScanner(
    private val lib: Library,
    private val db: Database,
) : Scanner {
    @OptIn(FlowPreview::class)
    override suspend fun scan() {
        val memoryIndex = ByteBuffersDirectory()
        val analyzer = StandardAnalyzer()
        val start = Clock.System.now()
        lib.rootPaths.forEach { root ->
            File(root)
                .walk()
                .filter {
                    !it.name.startsWith(".") &&
                        it.isFile &&
                        !it.isSubtitleFile() &&
                        !it.isImageFile() &&
                        !it.isNfoFile()
                }.asFlow()
                .parMapUnordered { movieFile ->
                    coRunCatching {
                        withTimeoutOrNull(10.minutes) {
                            val id = movieFile.id(lib)
                            val curr = db.getMovie(id)

                            val nfo =
                                Path(movieFile.parent, "${movieFile.nameWithoutExtension}.nfo")
                                    .let {
                                        log.info { it }
                                        if (it.exists()) it else null
                                    }?.readText()
                                    ?.parseMovieNfo()
                            val subtitleFiles =
                                movieFile.parentFile
                                    .walk()
                                    .maxDepth(1)
                                    .filter {
                                        it.isSubtitleFile() &&
                                            it.name.startsWith(movieFile.nameWithoutExtension)
                                    }.associate { it.nameWithoutExtension to FileLocation.LocalFile(it.absolutePath) }

                            // TODO skip subtitle track ffprobing if it hasn't changed
                            val subtitleFileTracks =
                                subtitleFiles
                                    .mapValues { entry ->
                                        FfprobeResult
                                            .parseFile(Path(entry.value.path).toFile())
                                            .extractInfo()
                                            .subtitleTracks.values
                                            .firstOrNull()
                                            ?.let {
                                                SubtitleFileTrack(entry.value, it)
                                            }
                                    }.mapValuesNotNull { it.value }
                            val posterImageFile = movieFile.findMediaImage(lib, "-poster")
                                ?: movieFile.findMediaImage(lib)
                            val backdropImageFile = movieFile.findMediaImage(lib, "-backdrop")
                            val lastModified = movieFile.lastModified()
                            val ffprobe =
                                curr?.let {
                                    if (lastModified < it.lastUpdated.toEpochMilliseconds()) {
                                        it.toFfprobeInfo()
                                    } else {
                                        null
                                    }
                                } ?: withTimeoutOrNull(15.seconds) {
                                    log.info { "Probing ${movieFile.absolutePath}" }
                                    FfprobeResult.parseFile(movieFile).extractInfo().also {
                                        log.info { "Done Probing ${movieFile.absolutePath}" }
                                    }
                                }
                            if (ffprobe != null && ffprobe.videoTracks.isNotEmpty() && ffprobe.runTime > 1.0) {
                                val title = nfo?.title ?: movieFile.nameWithoutExtension
                                MovieScanResults(
                                    images = setOfNotNull(posterImageFile, backdropImageFile),
                                    movies =
                                    setOf(
                                        ServerMovieInfo(
                                            id = id,
                                            libraryId = lib.name,
                                            audioTracks = ffprobe.audioTracks,
                                            videoTracks = ffprobe.videoTracks,
                                            subtitleTracks = ffprobe.subtitleTracks,
                                            title = title,
                                            runTime = ffprobe.runTime,
                                            plot = nfo?.plot,
                                            outline = nfo?.outline,
                                            directors = nfo?.director,
                                            writers = nfo?.writer,
                                            credits = nfo?.credits,
                                            rating = nfo?.rating,
                                            year = nfo?.year,
                                            backdropImageId = backdropImageFile?.imageId,
                                            posterImageId = posterImageFile?.imageId,
                                            fileInfo =
                                            FileInfo(
                                                location = FileLocation.LocalFile(movieFile.absolutePath),
                                                size =
                                                withContext(Dispatchers.IO) {
                                                    Files.size(movieFile.toPath())
                                                },
                                            ),
                                            subtitleFileTracks = subtitleFileTracks,
                                            lastModified = lastModified.let(Instant.Companion::fromEpochMilliseconds),
                                            lastUpdated = Clock.System.now(),
                                        ),
                                    ),
                                )
                            } else {
                                null
                            }
                        }
                    }.getOrNull()
                }.filterNotNull()
                .flowOn(Dispatchers.IO)
                .fold(
                    MovieScanResults.EMPTY,
                    MovieScanResults::plus,
                ).let { results ->
                    results.images.mapNotNull {
                        try {
                            db.upsertImage(it)
                            null // TODO may want to index these at some point
                        } catch (e: Exception) {
                            log.error(e) { "Failed to upsert image $it" }
                            null
                        }
                    } + results.movies.mapNotNull {
                        try {
                            db.upsertMovie(it)
                            it.toDocument()
                        } catch (e: Exception) {
                            log.error(e) { "Failed to upsert movie $it" }
                            null
                        }
                    }
                }.fold(
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

        db.cleanMovies(start, lib.name)
        db.cleanImages(start, lib.name)
    }

    companion object {
        private val log = KotlinLogging.logger { }

        private val xmlMapper by lazy {
            XmlMapper().apply {
                registerModule(
                    KotlinModule.Builder().build(),
                )
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }

        private fun String.parseMovieNfo(): MovieNfo? =
            try {
                xmlMapper.readValue(this.trim())
            } catch (e: Exception) {
                log.error(e) { "Failed to parse XML: $this" }
                null
            }

        private fun ServerMovieInfo.toFfprobeInfo(): FfprobeInfo =
            FfprobeInfo(
                this.audioTracks,
                this.videoTracks,
                this.subtitleTracks,
                this.runTime,
            )

        private fun ServerMovieInfo.toDocument() =
            Document().apply {
                identity(this@toDocument.id)
                add(StringField("title", this@toDocument.title, Field.Store.YES))
                add(StoredField("id", this@toDocument.id))
                add(StoredField("description", this@toDocument.plot ?: this@toDocument.outline ?: ""))
                add(StoredField("type", SearchResultType.Movie.name))
            }
    }
}
