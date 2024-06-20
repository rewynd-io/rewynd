package io.rewynd.worker.stream

import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.common.model.StreamProps
import io.rewynd.common.model.SubtitleSegment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val log by lazy { KotlinLogging.logger { } }

data class Cue(val start: Duration, val end: Duration, val content: String)

data class CueState(val cues: List<Cue>, val styles: List<String>, val startOffset: Duration = Duration.ZERO)

sealed interface InProgressCue {
    data class Cue(val start: Duration, val end: Duration, val content: String) : InProgressCue

    data class Style(val content: String) : InProgressCue
}

@Suppress("MagicNumber")
fun String.parseDuration() =
    this.trim().split(":").let {
        try {
            when (it.size) {
                3 -> it[0].toInt().hours + it[1].toInt().minutes + it[2].toDouble().seconds
                2 -> it[0].toInt().minutes + it[1].toDouble().seconds
                else -> null
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to parse duration from '$this'" }
            null
        }
    }

fun CueState.push(complete: InProgressCue?) =
    when (complete) {
        null -> this
        is InProgressCue.Cue ->
            copy(
                cues = cues + listOf(Cue(complete.start, complete.end, complete.content)),
            )

        is InProgressCue.Style -> copy(styles = styles + listOf(complete.content))
    }

fun List<Cue>.format() =
    "WEBVTT\nX-TIMESTAMP-MAP=MPEGTS:0,LOCAL:00:00:00.000\n\n\n\n" +
        joinToString("\n\n\n") { cue ->
            val startHours = cue.start.inWholeHours
            val startMinutes = (cue.start - startHours.hours).inWholeMinutes
            val startSecs = (cue.start - startHours.hours - startMinutes.minutes).inWholeSeconds
            val startMillis =
                (cue.start - startHours.hours - startMinutes.minutes - startSecs.seconds).inWholeMilliseconds
            val endHours = cue.end.inWholeHours
            val endMinutes = (cue.end - endHours.hours).inWholeMinutes
            val endSecs = (cue.end - endHours.hours - endMinutes.minutes).inWholeSeconds
            val endMillis = (cue.end - endHours.hours - endMinutes.minutes - endSecs.seconds).inWholeMilliseconds
            "${
                startHours.takeIf {
                    it != 0L
                    true
                }?.let { "${it.leftPad()}:" } ?: ""
            }${startMinutes.leftPad()}:${startSecs.leftPad()}.${startMillis.rightPad()} --> ${
                endHours.takeIf {
                    it != 0L
                    true
                }?.let { "${it.leftPad()}:" } ?: ""
            }${endMinutes.leftPad()}:${endSecs.leftPad()}.${endMillis.rightPad()}\n${cue.content.trim()}"
        } +
        "\n"

private fun processSubtitleLine(
    line: String,
    state: CueState,
    inProgress: InProgressCue?,
) = if (line.trim() == "STYLE") {
    state to InProgressCue.Style("")
} else if (line.trim() == "REGION") {
    state.push(inProgress) to null
} else if (line.trim() == "NOTE") {
    state.push(inProgress) to null
} else if (line.contains("-->")) {
    val timestamps = line.split("-->")
    if (timestamps.size == 2) {
        timestamps[0].parseDuration()?.let { start ->
            timestamps[1].parseDuration()?.let { end ->
                state.push(inProgress) to
                    InProgressCue.Cue(
                        start,
                        end,
                        "",
                    )
            }
        } ?: (state to inProgress)
    } else {
        state to inProgress
    }
} else if (inProgress != null) {
    when (inProgress) {
        is InProgressCue.Style -> {
            state to
                inProgress.copy(
                    content = inProgress.content + "\n" + line,
                )
        }

        is InProgressCue.Cue -> {
            state to
                inProgress.copy(
                    content = inProgress.content + "\n" + line,
                )
        }
    }
} else {
    state to null
}

fun CoroutineScope.launchSubtitleJob(
    streamProps: StreamProps,
    metadataHelper: StreamMetadataHelper,
) = launch(Dispatchers.IO) {
    // Here
    log.info { "Started subtitle job" }
    if (streamProps.subtitleStreamName != null) {
        val args = mkSubtitleCommand(streamProps)
        if (args != null) {
            log.info { "Running: ${args.joinToString(" ")}" }
            val pb = ProcessBuilder(*args.toTypedArray())
            val process = pb.start()
            try {
                process.inputReader().useLines { lines ->
                    lines.fold(
                        CueState(
                            emptyList(),
                            emptyList(),
                        ) to null as InProgressCue?,
                    ) { (state, inProgress), line ->
                        val (newState, newProgress) = processSubtitleLine(line, state, inProgress)
                        val cues = newState.cues
                        val end = (cues.lastOrNull()?.end ?: Duration.ZERO)
                        val newStartOffset = newState.startOffset + 10.seconds
                        val trimmedNewState =
                            if (newStartOffset < end) {
                                val segmentCues = cues.takeSegment(newStartOffset)
                                val remaining = cues.takeRemaining(newStartOffset)

                                metadataHelper.addSubtitleSegment(
                                    SubtitleSegment(10.seconds, segmentCues.format()),
                                )
                                newState.copy(cues = remaining, startOffset = newStartOffset)
                            } else {
                                newState
                            }

                        trimmedNewState to newProgress
                    }.let { (cueState, inProgress) ->
                        val finalCueState = cueState.push(inProgress)
                        val duration =
                            finalCueState.cues.lastOrNull()?.let {
                                it.end - finalCueState.startOffset
                            }
                        duration?.let {
                            metadataHelper.addSubtitleSegment(
                                SubtitleSegment(
                                    it,
                                    finalCueState.cues.map { cue ->
                                        cue.copy(
                                            start = cue.start,
                                            end = cue.end,
                                        )
                                    }.format(),
                                ),
                            )
                        }
                        metadataHelper.completeSubtitles()
                    }
                }
                log.info { "Ended subtitle job" }
            } catch (e: Exception) {
                log.error(e) { "Subtitle Job failed" }
                throw CancellationException("Subtitle Job Failed", e)
            } finally {
                process.destroy()
            }
        }
    }
}

private fun List<Cue>.takeSegment(newStartOffset: Duration) =
    takeWhile { newStartOffset > it.end }.map {
        val segEnd =
            it.end.inWholeMicroseconds.coerceAtMost(
                newStartOffset.inWholeMicroseconds,
            ).microseconds
        val segStart =
            it.start.inWholeMicroseconds.coerceAtMost(
                segEnd.inWholeMicroseconds,
            ).microseconds
        it.copy(
            start = segStart,
            end = segEnd,
        )
    }

private fun List<Cue>.takeRemaining(newStartOffset: Duration) =
    takeLastWhile { newStartOffset < it.end }.map {
        val segStart =
            it.start.inWholeMicroseconds.coerceAtLeast(
                newStartOffset.inWholeMicroseconds,
            ).microseconds
        val segEnd =
            it.end.inWholeMicroseconds.coerceAtLeast(
                segStart.inWholeMicroseconds,
            ).microseconds
        it.copy(
            start = segStart,
            end = segEnd,
        )
    }

private fun mkSubtitleCommand(streamProps: StreamProps): List<String>? {
    val track = streamProps.mediaInfo.subtitleTracks[streamProps.subtitleStreamName]
    val file = streamProps.mediaInfo.subtitleFiles[streamProps.subtitleStreamName]
    val args =
        if (file != null) {
            listOf(
                "ffmpeg",
                "-loglevel",
                "quiet",
                "-accurate_seek",
                "-ss",
                streamProps.startOffset.partialSeconds,
                "-i",
                file.toFfmpegUri()
            ) +
                FFMPEG_ACCURATE +
                listOf(
                    "-c:s:0",
                    "webvtt",
                    "-f",
                    "webvtt",
                    "pipe:1",
                )
        } else if (track != null) {
            listOf(
                "ffmpeg",
                "-loglevel",
                "quiet",
                "-accurate_seek",
                "-ss",
                streamProps.startOffset.partialSeconds,
                "-i",
                streamProps.mediaInfo.fileInfo.location.toFfmpegUri()
            ) +
                FFMPEG_ACCURATE +
                listOf(
                    "-c:s:${track.index}",
                    "webvtt",
                    "-f",
                    "webvtt",
                    "pipe:1",
                )
        } else {
            null
        }
    return args
}

private fun Long.leftPad(length: Int = 2) =
    toString().let {
        (if (it.length < length) "0".repeat(length - it.length) else "") + it
    }

private fun Long.rightPad(length: Int = 3) =
    toString().let {
        it + (if (it.length < length) "0".repeat(length - it.length) else "")
    }
