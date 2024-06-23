package io.rewynd.worker.stream

import io.rewynd.common.model.FileLocation

fun FileLocation.toFfmpegUri() =
    when (this) {
        is FileLocation.LocalFile -> path
    }
