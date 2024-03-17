package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.rewynd.common.cache.Cache
import io.rewynd.common.cache.queue.ImageJobQueue
import io.rewynd.common.cache.queue.WorkerEvent
import io.rewynd.common.database.Database
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.hours

fun Route.imageRoutes(
    db: Database,
    cache: Cache,
    queue: ImageJobQueue,
) {
    get("/image/{imageId}") {
        call.parameters["imageId"]?.let { imageId ->
            val cachedImage = cache.getImage(imageId)
            if (cachedImage != null) {
                cache.expireImage(imageId, Clock.System.now() + 1.hours)
                call.respond(cachedImage)
            } else {
                val imageInfo = db.getImage(imageId)
                if (imageInfo == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    queue.submit(imageInfo)
                        .let { jobId ->
                            queue.monitor(jobId)
                                .filter { it is WorkerEvent.Success || it is WorkerEvent.Fail }
                        }.first()
                        .let {
                            when (it) {
                                is WorkerEvent.Success -> call.respond(Json.decodeFromString<ByteArray>(it.payload))
                                else -> call.respond(HttpStatusCode.InternalServerError)
                            }
                        }
                }
            }
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
}
