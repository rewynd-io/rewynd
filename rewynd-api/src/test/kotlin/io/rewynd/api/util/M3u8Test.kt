package io.rewynd.api.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import io.rewynd.test.ApiGenerators
import io.rewynd.test.InternalGenerators

internal class M3u8Test : StringSpec({
    "index" {
        checkAll(InternalGenerators.streamMetadata, ApiGenerators.streamId) { metadata, streamId ->
            metadata.toIndexM3u8(streamId) shouldBe
                if (metadata.subtitles != null) {
                    val codecs = (metadata.mime.codecs + listOf("wvtt")).joinToString(", ")
                    """
                    #EXTM3U
                    #EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID="subs",CHARACTERISTICS="public.accessibility.transcribes-spoken-dialog",NAME="English",AUTOSELECT=YES,DEFAULT=YES,FORCED=YES,LANGUAGE="en-US",URI="subs.m3u8"
                    #EXT-X-STREAM-INF:BANDWIDTH=1924009,CODECS="$codecs",SUBTITLES="subs"
                    /api/stream/$streamId/stream.m3u8
                    """.trimIndent()
                } else {
                    val codecs = metadata.mime.codecs.joinToString(", ")
                    """
                    #EXTM3U
                    #EXT-X-STREAM-INF:BANDWIDTH=1924009,CODECS="$codecs"
                    /api/stream/$streamId/stream.m3u8
                    """.trimIndent()
                }
        }
    }

    "subtitles" {
        checkAll(InternalGenerators.subtitleMetadata) {
            val targetDuration =
                (
                    it.segments.maxOfOrNull { seg ->
                        seg.duration.inWholeMilliseconds
                    } ?: 10000L
                ).toDouble() / 1000.0
            val segments =
                it.segments.mapIndexed { index, seg -> index to seg }
                    .joinToString("") { (index, seg) ->
                        """
                        #EXTINF:${seg.duration.inWholeMilliseconds.toDouble() / 1000.0},
                        $index.vtt
                        """.trimIndent().plus("\n")
                    }
            val complete =
                if (it.complete) {
                    "#EXT-X-ENDLIST\n"
                } else {
                    ""
                }
            it.toSubsM3u8() shouldBe
                """
                #EXTM3U
                #EXT-X-VERSION:7
                #EXT-X-TARGETDURATION:$targetDuration
                #EXT-X-MEDIA-SEQUENCE:0
                """.trimIndent().plus("\n") + segments + complete
        }
    }

    "stream" {
        checkAll(InternalGenerators.streamMetadata) {
            val segments =
                it.segments.mapIndexed { index, seg -> index to seg }
                    .joinToString("") { (index, seg) ->
                        """
                        #EXTINF:${seg.duration.inWholeMicroseconds.toDouble() / 1000000.0},
                        $index.m4s
                        """.trimIndent().plus("\n")
                    }
            val complete =
                if (it.complete) {
                    "#EXT-X-ENDLIST\n"
                } else {
                    ""
                }
            it.toStreamM3u8() shouldBe
                """
                #EXTM3U
                #EXT-X-VERSION:7
                #EXT-X-PLAYLIST-TYPE:EVENT
                #EXT-X-TARGETDURATION:${it.segments.maxOfOrNull { segment -> segment.duration.inWholeSeconds } ?: 10L}
                #EXT-X-MEDIA-SEQUENCE:0
                #EXT-X-MAP:URI="init-stream.mp4"
                """.trimIndent().plus("\n") + segments + complete
        }
    }
})
