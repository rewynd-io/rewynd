package io.rewynd.worker.ffprobe

import io.rewynd.common.model.ServerAudioTrack
import io.rewynd.common.model.ServerSubtitleTrack
import io.rewynd.common.model.ServerVideoTrack
import io.rewynd.worker.execToString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class FfprobeResult(
    @SerialName("streams") var streams: List<Streams> = emptyList(),
    @SerialName("chapters") var chapters: List<Chapters> = emptyList(),
    @SerialName("format") var format: Format? = Format(),
) {

    fun extractInfo(): FfprobeInfo {
        val audioTracks =
            this.streams.filter { it.codecType == "audio" }.associate {
                val name = "${it.index} - ${it.codecName} - ${it.tags?.language}"

                name to
                        ServerAudioTrack(
                            id = it.id,
                            index = it.index,
                            codecName = it.codecName,
                            codecLongName = it.codecLongName,
                            profile = it.profile,
                            codecType = it.codecType,
                            codecTagString = it.codecTagString,
                            codecTag = it.codecTag,
                            width = it.width,
                            height = it.height,
                            codedWidth = it.codedWidth,
                            codedHeight = it.codedHeight,
                            closedCaptions = it.closedCaptions,
                            filmGrain = it.filmGrain,
                            hasBFrames = it.hasBFrames,
                            sampleAspectRatio = it.sampleAspectRatio,
                            displayAspectRatio = it.displayAspectRatio,
                            pixFmt = it.pixFmt,
                            level = it.level,
                            colorRange = it.colorRange,
                            chromaLocation = it.chromaLocation,
                            refs = it.refs,
                            rFrameRate = it.rFrameRate,
                            avgFrameRate = it.avgFrameRate,
                            timeBase = it.timeBase,
                            startPts = it.startPts,
                            duration = it.duration,
                            durationTs = it.durationTs,
                            startTime = it.startTime,
                            extradataSize = it.extradataSize,
                            default = it.disposition?.default,
                            dub = it.disposition?.dub,
                            original = it.disposition?.original,
                            comment = it.disposition?.comment,
                            lyrics = it.disposition?.lyrics,
                            karaoke = it.disposition?.karaoke,
                            forced = it.disposition?.forced,
                            hearingImpaired = it.disposition?.hearingImpaired,
                            visualImpaired = it.disposition?.visualImpaired,
                            cleanEffects = it.disposition?.cleanEffects,
                            attachedPic = it.disposition?.attachedPic,
                            timedThumbnails = it.disposition?.timedThumbnails,
                            captions = it.disposition?.captions,
                            descriptions = it.disposition?.descriptions,
                            metadata = it.disposition?.metadata,
                            dependent = it.disposition?.dependent,
                            stillImage = it.disposition?.stillImage,
                            creationTime = it.tags?.creationTime,
                            language = it.tags?.language,
                            encoder = it.tags?.encoder,
                        )
            }
        val videoTracks =
            this.streams.filter { it.codecType == "video" }.associate {
                val name = "${it.index} - ${it.codecName} - ${it.tags?.language}"

                name to
                        ServerVideoTrack(
                            id = it.id,
                            index = it.index,
                            codecName = it.codecName,
                            codecLongName = it.codecLongName,
                            profile = it.profile,
                            codecType = it.codecType,
                            codecTagString = it.codecTagString,
                            codecTag = it.codecTag,
                            width = it.width,
                            height = it.height,
                            codedWidth = it.codedWidth,
                            codedHeight = it.codedHeight,
                            closedCaptions = it.closedCaptions,
                            filmGrain = it.filmGrain,
                            hasBFrames = it.hasBFrames,
                            sampleAspectRatio = it.sampleAspectRatio,
                            displayAspectRatio = it.displayAspectRatio,
                            pixFmt = it.pixFmt,
                            level = it.level,
                            colorRange = it.colorRange,
                            chromaLocation = it.chromaLocation,
                            refs = it.refs,
                            rFrameRate = it.rFrameRate,
                            avgFrameRate = it.avgFrameRate,
                            timeBase = it.timeBase,
                            startPts = it.startPts,
                            duration = it.duration,
                            durationTs = it.durationTs,
                            startTime = it.startTime,
                            extradataSize = it.extradataSize,
                            default = it.disposition?.default,
                            dub = it.disposition?.dub,
                            original = it.disposition?.original,
                            comment = it.disposition?.comment,
                            lyrics = it.disposition?.lyrics,
                            karaoke = it.disposition?.karaoke,
                            forced = it.disposition?.forced,
                            hearingImpaired = it.disposition?.hearingImpaired,
                            visualImpaired = it.disposition?.visualImpaired,
                            cleanEffects = it.disposition?.cleanEffects,
                            attachedPic = it.disposition?.attachedPic,
                            timedThumbnails = it.disposition?.timedThumbnails,
                            captions = it.disposition?.captions,
                            descriptions = it.disposition?.descriptions,
                            metadata = it.disposition?.metadata,
                            dependent = it.disposition?.dependent,
                            stillImage = it.disposition?.stillImage,
                            creationTime = it.tags?.creationTime,
                            language = it.tags?.language,
                            encoder = it.tags?.encoder,
                        )
            }

        val subtitleTracks =
            this.streams.filter {
                it.codecType == "subtitle" &&
                        subtitleCodecs.contains(
                            it.codecName,
                        )
            }.associate {
                val name = "${it.index} - ${it.codecName} - ${it.tags?.language}"

                name to
                        ServerSubtitleTrack(
                            id = it.id,
                            index = it.index,
                            codecName = it.codecName,
                            codecLongName = it.codecLongName,
                            profile = it.profile,
                            codecType = it.codecType,
                            codecTagString = it.codecTagString,
                            codecTag = it.codecTag,
                            width = it.width,
                            height = it.height,
                            codedWidth = it.codedWidth,
                            codedHeight = it.codedHeight,
                            closedCaptions = it.closedCaptions,
                            filmGrain = it.filmGrain,
                            hasBFrames = it.hasBFrames,
                            sampleAspectRatio = it.sampleAspectRatio,
                            displayAspectRatio = it.displayAspectRatio,
                            pixFmt = it.pixFmt,
                            level = it.level,
                            colorRange = it.colorRange,
                            chromaLocation = it.chromaLocation,
                            refs = it.refs,
                            rFrameRate = it.rFrameRate,
                            avgFrameRate = it.avgFrameRate,
                            timeBase = it.timeBase,
                            startPts = it.startPts,
                            duration = it.duration,
                            durationTs = it.durationTs,
                            startTime = it.startTime,
                            extradataSize = it.extradataSize,
                            default = it.disposition?.default,
                            dub = it.disposition?.dub,
                            original = it.disposition?.original,
                            comment = it.disposition?.comment,
                            lyrics = it.disposition?.lyrics,
                            karaoke = it.disposition?.karaoke,
                            forced = it.disposition?.forced,
                            hearingImpaired = it.disposition?.hearingImpaired,
                            visualImpaired = it.disposition?.visualImpaired,
                            cleanEffects = it.disposition?.cleanEffects,
                            attachedPic = it.disposition?.attachedPic,
                            timedThumbnails = it.disposition?.timedThumbnails,
                            captions = it.disposition?.captions,
                            descriptions = it.disposition?.descriptions,
                            metadata = it.disposition?.metadata,
                            dependent = it.disposition?.dependent,
                            stillImage = it.disposition?.stillImage,
                            creationTime = it.tags?.creationTime,
                            language = it.tags?.language,
                            encoder = it.tags?.encoder,
                        )
            }

        return FfprobeInfo(audioTracks, videoTracks, subtitleTracks, this.duration())
    }

    private fun FfprobeResult.duration() = this.format?.duration ?: this.streams.mapNotNull {
        it.duration
    }.maxOrNull() ?: 0.0


    companion object {
        val json = Json { ignoreUnknownKeys = true }
        private val subtitleCodecs = setOf("subrip", "srt", "webvtt", "wvtt")

        fun parseFile(file: File) =
            json.decodeFromString<FfprobeResult>(
                listOf(
                    "ffprobe",
                    "-v",
                    "quiet",
                    "-print_format",
                    "json",
                    "-show_error",
                    "-show_format",
                    "-show_streams",
                    "-show_chapters",
                    "${file.absolutePath}",
                ).execToString(),
            )
    }
}
