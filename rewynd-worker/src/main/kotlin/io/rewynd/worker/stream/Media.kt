package io.rewynd.worker.stream

import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.JobContext
import io.rewynd.common.model.ClientStreamEvents
import io.rewynd.common.model.Mime
import io.rewynd.common.model.ServerAudioTrack
import io.rewynd.common.model.ServerVideoTrack
import io.rewynd.common.model.StreamProps
import io.rewynd.common.model.WorkerStreamEvents
import io.rewynd.model.NormalizationMethod
import io.rewynd.model.NormalizationProps
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.kensand.margarita.Mp4Frag
import okio.source
import kotlin.time.Duration.Companion.hours

private val log by lazy { KotlinLogging.logger { } }

fun CoroutineScope.launchMediaJob(
    streamProps: StreamProps,
    context: JobContext<StreamProps, Unit, ClientStreamEvents, WorkerStreamEvents>,
    metadataHelper: StreamMetadataHelper,
    cache: Cache,
) = launch(Dispatchers.IO, CoroutineStart.LAZY) {
    val args = streamProps.ffmpegArgs()
    log.info { "Running: ${args.joinToString(" ")}" }
    val pb = ProcessBuilder(*args.toTypedArray())
    val process = pb.start()
    val progressJob =
        launch(Dispatchers.IO) {
            try {
                process.errorReader().lineSequence().asFlow().takeWhile {
                    currentCoroutineContext().isActive
                }.runningFold(emptyMap<String, String>()) { acc, value ->
                    val mapping = value.split("=")
                    if (mapping.size == 2) {
                        acc + mapOf(mapping[0] to mapping[1])
                    } else {
                        acc
                    }
                }.flowOn(Dispatchers.IO).collect {
                    context.workerEventEmitter(WorkerStreamEvents.Progress(it))
                }
            } catch (e: CancellationException) {
                log.warn(e) { "Cancelling Progress Job" }
                throw e
            } catch (e: Exception) {
                log.error(e) { "Progress job encountered error" }
            }
        }
    try {
        Mp4Frag.read(process.inputStream.source()).fold(metadataHelper) { acc, event ->
            when (event) {
                is Mp4Frag.Event.Init -> {
                    cache.putInitMp4(
                        streamProps.id,
                        event.data.toByteArray(),
                        Clock.System.now() + 1.hours,
                    )
                    acc.init(
                        Mime(
                            if (event.videoCodec != null) "video/mp4" else "audio/mp4",
                            listOfNotNull(event.videoCodec, event.audioCodec),
                        ),
                    )
                }

                is Mp4Frag.Event.Segment -> {
                    val segmentIndex = acc.addSegment(event.duration)
                    cache.putSegmentM4s(
                        streamProps.id,
                        segmentIndex,
                        event.data.toByteArray(),
                        Clock.System.now() + 1.hours,
                    )
                }

                is Mp4Frag.Event.Error -> {
                    throw RuntimeException("Mp4 fragmentation failed for ${context.jobId}", event.cause)
                }
            }
            acc
        }.also {
            it.complete()
        }
    } catch (e: CancellationException) {
        log.warn(e) { "Mp4frag was cancelled" }
        throw e
    } catch (e: Exception) {
        log.error(e) { "Mp4frag failed" }
    } finally {
        progressJob.cancel()
        process.destroy()
    }
}

private fun StreamProps.ffmpegArgs() =
    (
        FFMPEG_START +
            startLocation +
            fileLocation +
            FFMPEG_ACCURATE +
            mkVideoTrackProps +
            mkAudioTrackProps +
            FFMPEG_END
        )

val ServerVideoTrack.key: String
    get() = "-c:v:${this.index}"
val ServerAudioTrack.key: String
    get() = "-c:a:${this.index}"
val ServerAudioTrack.filterKey: String
    get() = "-filter:a:${this.index}"
val ServerVideoTrack.defaultVideoTrackProps: List<String>
    get() =
        listOf(
            key,
            "h264",
            "-preset",
            "medium",
            "-crf",
            "28",
            "-pix_fmt",
            "yuv420p",
        )

private val StreamProps.fileLocation
    get() = listOf("-i", mediaInfo.fileInfo.location.toFfmpegUri())
val FFMPEG_START =
    listOf(
        "ffmpeg",
        "-loglevel",
        "quiet",
        "-progress",
        "pipe:2",
        // TODO parse these from error output and add them to metadata
        "-nostats",
        // TODO nvidia hwaccel actually appears to be as simple as
        // "-hwaccel", "cuda",
        "-probesize",
        "1000000",
        "-analyzeduration",
        "1000000",
        "-accurate_seek",
    )

// Used for combined seeking per: https://trac.ffmpeg.org/wiki/Seeking#Combined
val FFMPEG_ACCURATE = listOf("-ss", "0")

val FFMPEG_END =
    listOf(
        "-f",
        "mp4",
        "-ac",
        // TODO support more than just stereo audio streams
        "2",
        "-movflags",
        "+delay_moov+frag_keyframe+empty_moov+default_base_moof",
        "pipe:1",
    )

val StreamProps.startLocation: List<String>
    get() = listOf("-ss", startOffset.partialSeconds)

val StreamProps.mkVideoTrackProps: List<String>
    get() =
        videoStreamName?.let {
            mediaInfo.videoTracks[it]?.mkVideoTrackProps
        } ?: listOf(
            "-vf",
            "drawbox=color=black:t=fill",
            "-video_size",
            "1x1",
        )

val ServerVideoTrack.mkVideoTrackProps
    get() =
        when (this.codecName?.lowercase()) {
            "av1" -> mkAv1TrackProps
            "h264" -> mkH264TrackProps
            "h265" -> mkH265TrackProps
            "hevc" -> mkHevcTrackProps
            else -> defaultVideoTrackProps
        }

val ServerVideoTrack.mkAv1TrackProps
    get() =
        if (pixFmt?.lowercase() in supportedPixelFormats) {
            listOf(key, "copy")
        } else {
            defaultVideoTrackProps
        }

val ServerVideoTrack.mkH264TrackProps
    get() =
        if (pixFmt?.lowercase() in supportedPixelFormats) {
            listOf(key, "copy")
        } else {
            defaultVideoTrackProps
        }

val ServerVideoTrack.mkHevcTrackProps get() = defaultVideoTrackProps

val ServerVideoTrack.mkH265TrackProps get() = defaultVideoTrackProps

val supportedPixelFormats =
    setOf(
        "yuv420p",
        "yuv422p",
        "yuv420p10le",
        "yuv422p10le",
        "yuv444p",
        "yuv444p10le",
    )

val StreamProps.mkAudioTrackProps: List<String>
    get() =
        audioStreamName?.let {
            mediaInfo.audioTracks[it]?.let { audioTrack ->
                when (audioTrack.codecName?.lowercase()) {
                    "aac",
                    "ac3",
                    "vorbis",
                    "mp3",
                    -> listOf(audioTrack.key, "copy")

                    else -> listOf(audioTrack.key, "aac")
                } + (normalization?.mkNormalizationProps(audioTrack) ?: emptyList())
            }
        } ?: listOf(
            "-an",
        )

private fun NormalizationProps.mkNormalizationProps(audioTrack: ServerAudioTrack): List<String> =
    when (method) {
        NormalizationMethod.loudnorm -> listOf(audioTrack.filterKey, "loudnorm")
    }
