package io.rewynd.worker.stream

import io.rewynd.common.model.FileLocation
import kotlin.time.Duration

val Duration.partialSeconds: String
    get() = "%.4f".format(inWholeMilliseconds.toDouble() / 1000.0)

fun FileLocation.toFfmpegUri() =
    when (this) {
        is FileLocation.LocalFile -> path
    }
