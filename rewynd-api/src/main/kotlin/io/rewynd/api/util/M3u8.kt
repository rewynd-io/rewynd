package io.rewynd.api.util

import io.rewynd.common.indexed
import io.rewynd.common.model.StreamMetadata
import io.rewynd.common.model.SubtitleMetadata

fun SubtitleMetadata.toSubsM3u8(): String =
    with(StringBuilder()) {
        append("#EXTM3U\n")
        append("#EXT-X-VERSION:7\n")
        append(
            "#EXT-X-TARGETDURATION:${
                (
                    segments.maxOfOrNull {
                        it.duration.inWholeMilliseconds
                    } ?: 10000.0
                ).toDouble() / 1000.0
            }\n",
        )
        append("#EXT-X-MEDIA-SEQUENCE:0\n")

        segments.forEachIndexed { index, seg ->
            append(
                "#EXTINF:${seg.duration.inWholeMilliseconds.toDouble() / 1000.0},\n$index.vtt\n",
            )
        }

        if (complete) {
            append("#EXT-X-ENDLIST\n")
        }
        toString()
    }

fun StreamMetadata.toStreamM3u8(): String =
    segments.indexed().fold(
        StringBuilder(
            """
            #EXTM3U
            #EXT-X-VERSION:7
            #EXT-X-PLAYLIST-TYPE:EVENT
            #EXT-X-TARGETDURATION:${segments.maxOfOrNull { segment -> segment.duration }?.inWholeSeconds ?: 10L}
            #EXT-X-MEDIA-SEQUENCE:0
            #EXT-X-MAP:URI="init-stream.mp4"

            """.trimIndent(),
        ),
    ) { acc, (index, seg) ->
        acc.apply {
            append(
                "#EXTINF:${seg.duration.inWholeMicroseconds.toDouble() / 1000000.0},\n",
            )
            append("$index.m4s\n")
        }
    }.apply {
        if (complete) {
            append("#EXT-X-ENDLIST\n")
        }
    }.toString()

fun StreamMetadata.toIndexM3u8(streamId: String) =
    with(StringBuilder()) {
        append("#EXTM3U\n")
        subtitles?.let {
            append("#EXT-X-MEDIA:TYPE=SUBTITLES,")
            append("""GROUP-ID="subs",""")
            append("""CHARACTERISTICS="public.accessibility.transcribes-spoken-dialog",""")

            append("""NAME="English",""")
            append("""AUTOSELECT=YES,""")

            append("""DEFAULT=YES,""")
            append("""FORCED=YES,""")
            append("""LANGUAGE="en-US",""")

            append("""URI="subs.m3u8"""")

            append("\n")
        }
        append("#EXT-X-STREAM-INF:")
        append("BANDWIDTH=1924009,") // TODO actual correct bandwidth
        append(
            """CODECS="${
                (
                    mime.codecs +
                        listOfNotNull(
                            subtitles?.let {
                                "wvtt"
                            },
                        )
                ).joinToString(", ")
            }"""",
        )

        subtitles?.let {
            append(""",SUBTITLES="subs"""")
        }
        append("\n")
        append("""/api/stream/$streamId/stream.m3u8""")
        toString()
    }
