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
import io.rewynd.common.partialSecondsString
import io.rewynd.model.NormalizationMethod
import io.rewynd.model.NormalizationProps
import io.rewynd.worker.max
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private val log by lazy { KotlinLogging.logger { } }

fun findPriorKeyframe(streamProps: StreamProps) = streamProps.videoTrack?.let { videoTrack ->
    val startTime = max(streamProps.startOffset - 30.seconds, Duration.ZERO)
    val endTime = startTime + 30.seconds
    val args = listOf(
        "ffprobe",
        "-loglevel", "error",
        "-select_streams", "v:${videoTrack.index}",
        "-show_entries", "packet=pts_time,flags",
        "-of", "csv=print_section=0",
        "-read_intervals", "${startTime.partialSecondsString}%${endTime.partialSecondsString}",
        streamProps.mediaInfo.fileInfo.location.toFfmpegUri()
    )
    log.info { "Running: ${args.joinToString(" ")}" }
    val pb = ProcessBuilder(
        *args.toTypedArray()
    )
    val process = pb.start()
    process.inputReader().useLines { lines ->
        lines.map { it.split(",") }
            .filter { it.size == 2 && it[1].startsWith("K") }
            .mapNotNull { it.firstOrNull()?.toDoubleOrNull()?.seconds }
            .takeWhile { it <= streamProps.startOffset }
            .lastOrNull()
    }
} ?: streamProps.startOffset

fun CoroutineScope.launchMediaJob(
    streamProps: StreamProps,
    context: JobContext<StreamProps, Unit, ClientStreamEvents, WorkerStreamEvents>,
    metadataHelper: StreamMetadataHelper,
    cache: Cache,
) = launch(Dispatchers.IO, CoroutineStart.LAZY) {
    val args = streamProps.ffmpegArgs
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
                        streamProps.startOffset
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

private val StreamProps.ffmpegArgs
    get() = (
        FFMPEG_START +
            startLocation +
            fileLocation +
            mkVideoTrackProps +
            mkAudioTrackProps +
            FFMPEG_END
        )

val ServerAudioTrack.filterKey: String
    get() = "-filter:a:${this.index}"

val ServerVideoTrack.mkDefaultVideoTrackProps: List<String>
    get() =
        listOf(
            "-map",
            "0:$index",
            "-c:v",
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

val StreamProps.startLocation
    get() = listOf("-ss", startOffset.partialSecondsString)

val StreamProps.videoTrack: ServerVideoTrack?
    get() = videoStreamName?.let {
        mediaInfo.videoTracks[it]
    }

val StreamProps.audioTrack: ServerAudioTrack?
    get() = audioStreamName?.let {
        mediaInfo.audioTracks[it]
    }

val StreamProps.mkVideoTrackProps: List<String>
    get() = videoTrack?.mkVideoTrackProps ?: listOf(
        "-map",
        "0:v",
        "-vf",
        "drawbox=color=black:t=fill",
        "-video_size",
        "1x1",
    )

val ServerVideoTrack.videoCopyProps
    get() = listOf(
        "-map",
        "0:$index",
        "-c:v",
        "copy"
    )

val ServerVideoTrack.canCopy: Boolean
    get() = mkVideoTrackProps == videoCopyProps

val ServerVideoTrack.mkVideoTrackProps
    get() =
        when (this.codecName?.lowercase()) {
            "av1" -> mkAv1TrackProps
            "h264" -> mkH264TrackProps
            "h265" -> mkH265TrackProps
            "hevc" -> mkHevcTrackProps
            else -> mkDefaultVideoTrackProps
        }

val ServerVideoTrack.mkAv1TrackProps
    get() =
        if (pixFmt?.lowercase() in supportedPixelFormats) {
            videoCopyProps
        } else {
            mkDefaultVideoTrackProps
        }

val ServerVideoTrack.mkH264TrackProps
    get() =
        if (pixFmt?.lowercase() in supportedPixelFormats) {
            videoCopyProps
        } else {
            mkDefaultVideoTrackProps
        }

val ServerVideoTrack.mkHevcTrackProps get() = mkDefaultVideoTrackProps

val ServerVideoTrack.mkH265TrackProps get() = mkDefaultVideoTrackProps

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
        audioTrack?.let {
            it.mkAudioTrackProps + (normalization?.mkNormalizationProps(it) ?: emptyList())
        } ?: listOf("-an")

val ServerAudioTrack.mkAudioTrackProps
    get() = when (codecName?.lowercase()) {
        "aac",
        "ac3",
        "vorbis",
        "mp3",
        -> mkCopyAudioTrackProps

        else -> mkDefaultAudioTrackProps
    }

val ServerAudioTrack.mkCopyAudioTrackProps
    get() = listOf(
        "-map",
        "0:$index",
        "-c:a",
        "copy"
    )

val ServerAudioTrack.mkDefaultAudioTrackProps
    get() = listOf(
        "-map",
        "0:$index",
        "-c:a",
        "aac"
    )

private fun NormalizationProps.mkNormalizationProps(audioTrack: ServerAudioTrack): List<String> =
    when (method) {
        NormalizationMethod.loudnorm -> listOf(audioTrack.filterKey, "loudnorm")
    }
