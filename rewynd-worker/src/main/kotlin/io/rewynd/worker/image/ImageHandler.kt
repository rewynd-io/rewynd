package io.rewynd.worker.image

import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.ImageJobHandler
import io.rewynd.common.model.FileLocation
import kotlinx.datetime.Clock
import java.nio.file.Path
import kotlin.time.Duration.Companion.hours

fun mkImageJobHandler(cache: Cache): ImageJobHandler =
    { context ->
        when (val location = context.request.location) {
            is FileLocation.LocalFile -> Path.of(location.path).toFile().readBytes()
        }.also { cache.putImage(context.request.imageId, it, Clock.System.now() + 1.hours) }
    }
