package io.rewynd.worker.scan

import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.shouldBe
import kotlin.io.path.Path

class UtilKtTest :
    StringSpec({

        "isSubtitleFile" {
            table(
                headers("Path", "Result"),
                row("abc.def.srt", true),
                row("abc.srt", true),
                row("abc.defsrt", false),
                row("abc", false),
                row("abc.def.wvtt", true),
                row("abc.wvtt", true),
                row("abc.defwvtt", false),
                row("abc", false),
                row("abc.def.vtt", true),
                row("abc.vtt", true),
                row("abc.defvtt", false),
                row("abc", false),
            ).forAll { path, result ->
                Path(path).toFile().isSubtitleFile() shouldBe result
            }
        }

        "isImageFile" {
            table(
                headers("Path", "Result"),
                row("abc.def.png", true),
                row("abc.png", true),
                row("abc.defpng", false),
                row("abc", false),
                row("abc.def.jpg", true),
                row("abc.jpg", true),
                row("abc.defjpg", false),
                row("abc", false),
                row("abc.def.jpeg", true),
                row("abc.jpeg", true),
                row("abc.defjpeg", false),
                row("abc", false),
            ).forAll { path, result ->
                Path(path).toFile().isImageFile() shouldBe result
            }
        }
    })
