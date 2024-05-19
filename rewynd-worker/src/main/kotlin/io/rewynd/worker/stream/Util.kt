package io.rewynd.worker.stream

import io.rewynd.common.model.FileLocation
import kotlin.time.Duration

private const val MILLIS_IN_SECOND = 1000.0
val Duration.partialSeconds: String
    get() = "%.4f".format(inWholeMilliseconds.toDouble() / MILLIS_IN_SECOND)

fun FileLocation.toFfmpegUri() =
    when (this) {
        is FileLocation.LocalFile -> path
    }
