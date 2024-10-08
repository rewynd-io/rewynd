package io.rewynd.test

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.asString
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.kotlinx.datetime.date
import io.rewynd.common.cache.queue.JobId
import io.rewynd.common.model.FileInfo
import io.rewynd.common.model.FileLocation
import io.rewynd.common.model.LibraryData
import io.rewynd.common.model.Mime
import io.rewynd.common.model.Progressed
import io.rewynd.common.model.ServerAudioTrack
import io.rewynd.common.model.ServerEpisodeInfo
import io.rewynd.common.model.ServerImageInfo
import io.rewynd.common.model.ServerMediaInfo
import io.rewynd.common.model.ServerScanTask
import io.rewynd.common.model.ServerScheduleInfo
import io.rewynd.common.model.ServerSeasonInfo
import io.rewynd.common.model.ServerShowInfo
import io.rewynd.common.model.ServerSubtitleTrack
import io.rewynd.common.model.ServerUser
import io.rewynd.common.model.ServerVideoTrack
import io.rewynd.common.model.StreamMapping
import io.rewynd.common.model.StreamMetadata
import io.rewynd.common.model.StreamProps
import io.rewynd.common.model.StreamSegmentMetadata
import io.rewynd.common.model.SubtitleFileTrack
import io.rewynd.common.model.SubtitleMetadata
import io.rewynd.common.model.SubtitleSegment
import io.rewynd.common.model.UserProgress
import io.rewynd.model.NormalizationProps
import io.rewynd.test.ApiGenerators.actor
import io.rewynd.test.ApiGenerators.mediaInfo
import io.rewynd.test.ApiGenerators.seasonInfo
import net.kensand.kielbasa.kotest.property.Generators.boolean
import net.kensand.kielbasa.kotest.property.Generators.double
import net.kensand.kielbasa.kotest.property.Generators.duration
import net.kensand.kielbasa.kotest.property.Generators.instant
import net.kensand.kielbasa.kotest.property.Generators.int
import net.kensand.kielbasa.kotest.property.Generators.localDate
import net.kensand.kielbasa.kotest.property.Generators.long
import net.kensand.kielbasa.kotest.property.Generators.string
import net.kensand.kielbasa.kotest.property.Generators.urlEncodedBase64

object InternalGenerators {
    val libraryData =
        arbitrary {
            LibraryData(string.bind(), instant.bind())
        }

    val localFile = Arb.bind<FileLocation.LocalFile>()
    val fileLocation =
        Arb.choice(
            localFile,
        )
    val serverMediaInfo =
        arbitrary {
            ServerMediaInfo(
                mediaInfo = mediaInfo.bind(),
                libraryData = libraryData.bind(),
                fileInfo = fileInfo.bind(),
                videoTracks = (string to serverVideoTrack).map().bind(),
                audioTracks = (string to serverAudioTrack).map().bind(),
                subtitleTracks = (string to serverSubtitlesTrack).map().bind(),
                subtitleFiles = (string to fileLocation).map().bind(),
            )
        }
    val normalizationProps = Arb.bind<NormalizationProps>()
    val streamProps =
        arbitrary {
            StreamProps(
                id = string.bind(),
                mediaInfo = serverMediaInfo.bind(),
                audioStreamName = string.nullable().bind(),
                videoStreamName = string.nullable().bind(),
                subtitleStreamName = string.nullable().bind(),
                normalization = normalizationProps.bind(),
                startOffset = duration.bind(),
            )
        }
    val subtitleMetadata =
        arbitrary {
            SubtitleMetadata(
                segments = subtitleSegment.list(1..10).bind(),
                complete = boolean.bind(),
            )
        }
    val subtitleSegment =
        arbitrary {
            SubtitleSegment(
                duration = duration.bind(),
                content = string.bind(),
            )
        }
    val streamSegmentMetadata =
        arbitrary {
            StreamSegmentMetadata(
                duration = duration.bind(),
            )
        }
    val jobId = Arb.bind<JobId>()
    val mime = arbitrary { Mime(string.bind(), string.list().bind()) }
    val streamMetadata =
        arbitrary {
            StreamMetadata(
                streamProps = streamProps.bind(),
                segments = streamSegmentMetadata.list().bind(),
                subtitles = subtitleMetadata.nullable().bind(),
                mime = mime.bind(),
                complete = boolean.bind(),
                processed = duration.bind(),
                jobId = jobId.bind(),
                actualStartOffset = duration.bind()
            )
        }
    val serverUser =
        arbitrary {
            ServerUser(ApiGenerators.user.bind(), urlEncodedBase64.bind(), urlEncodedBase64.bind())
        }
    val fileInfo = Arb.bind<FileInfo>()
    val serverVideoTrack = Arb.bind<ServerVideoTrack>()
    val serverAudioTrack = Arb.bind<ServerAudioTrack>()
    val serverSubtitlesTrack = Arb.bind<ServerSubtitleTrack>()
    val subtitleFileTrack = Arb.bind<SubtitleFileTrack>()

    val serverEpisodeInfo =
        arbitrary {
            ServerEpisodeInfo(
                id = Codepoint.alphanumeric().bind().asString(), // TODO switch back to string.bind()
                libraryId = string.bind(),
                showId = string.bind(),
                seasonId = string.bind(),
                title = string.bind(),
                runTime = double.bind(),
                plot = string.nullable().bind(),
                outline = string.nullable().bind(),
                director = string.list().nullable().bind(),
                writer = string.list().nullable().bind(),
                credits = string.list().nullable().bind(),
                rating = double.nullable().bind(),
                year = int.nullable().bind(),
                episode = int.bind(),
                episodeNumberEnd = int.nullable().bind(),
                season = int.bind(),
                showName = string.bind(),
                aired = localDate.nullable().bind(),
                episodeImageId = string.nullable().bind(),
                fileInfo = fileInfo.bind(),
                videoTracks = (string to serverVideoTrack).map().bind(),
                audioTracks = (string to serverAudioTrack).map().bind(),
                subtitleTracks = (string to serverSubtitlesTrack).map().bind(),
                subtitleFileTracks = (string to subtitleFileTrack).map().bind(),
                lastUpdated = instant.bind(),
                lastModified = instant.bind(),
            )
        }

    val progressedServerEpisodeInfo = arbitrary {
        Progressed(serverEpisodeInfo.bind(), userProgress.bind())
    }

    val serverSeasonInfo =
        arbitrary {
            ServerSeasonInfo(
                seasonInfo = seasonInfo.bind(),
                libraryData = libraryData.bind(),
            )
        }

    val serverShowInfo =
        arbitrary {
            ServerShowInfo(
                id = Codepoint.alphanumeric().bind().asString(), // TODO switch back to string.bind()
                libraryId = string.bind(),
                title = string.bind(),
                plot = string.nullable().bind(),
                outline = string.nullable().bind(),
                originalTitle = string.nullable().bind(),
                premiered = localDate.nullable().bind(),
                releaseDate = localDate.nullable().bind(),
                endDate = localDate.nullable().bind(),
                mpaa = string.nullable().bind(),
                imdbId = string.nullable().bind(),
                tmdbId = string.nullable().bind(),
                tvdbId = string.nullable().bind(),
                tvRageId = string.nullable().bind(),
                rating = double.nullable().bind(),
                year = int.nullable().bind(),
                runTime = double.nullable().bind(),
                aired = localDate.nullable().bind(),
                genre = string.nullable().bind(),
                studio = string.nullable().bind(),
                status = string.nullable().bind(),
                tag = string.list().nullable().bind(),
                actors = actor.list().nullable().bind(),
                seriesImageId = string.nullable().bind(),
                backdropImageId = string.nullable().bind(),
                lastUpdated = instant.bind(),
            )
        }

    val serverImageInfo =
        arbitrary {
            ServerImageInfo(
                location = fileLocation.bind(),
                size = long.bind(),
                libraryId = string.bind(),
                lastUpdated = instant.bind(),
                imageId = Codepoint.alphanumeric().bind().asString(), // TODO switch back to string.bind()
            )
        }

    val userProgress =
        arbitrary {
            UserProgress(
                username = Codepoint.alphanumeric().bind().asString(), // TODO switch back to string.bind()
                id = Codepoint.alphanumeric().bind().asString(), // TODO switch back to string.bind()
                percent = Arb.double(0.0, 1.0).bind(),
                timestamp = instant.bind(),
            )
        }

    val serverScanTask =
        arbitrary {
            ServerScanTask(ApiGenerators.library.bind().name)
        }

    val serverScheduleInfo =
        arbitrary {
            ServerScheduleInfo(
                id = Codepoint.alphanumeric().bind().asString(), // TODO switch back to string.bind()
                cronExpression = "0 0 3 * * ? *", // TODO make ARB for cron expression
                scanTasks = serverScanTask.list().bind(),
            )
        }

    val streamMapping =
        arbitrary {
            StreamMapping(
                string.bind(),
                jobId.bind(),
            )
        }
}

fun <A> Arb<A>.nullable() =
    arbitrary {
        if (it.random.nextBoolean()) {
            this@nullable.bind()
        } else {
            null
        }
    }

fun <A> Arb<A>.list(range: IntRange = 0..10) = Arb.list(this, range)

fun <A, B> Pair<Arb<A>, Arb<B>>.map(range: IntRange = 0..10) = Arb.map(first, second, range.first, range.last)

fun <A, B> Collection<A>.uniqueBy(keyFun: (A) -> B) = associateBy(keyFun).values.toList()
