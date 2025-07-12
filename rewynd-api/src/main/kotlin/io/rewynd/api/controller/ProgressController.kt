package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.get
import io.rewynd.api.UserSession.Companion.withUsername
import io.rewynd.common.database.Database
import io.rewynd.common.model.UserProgress
import io.rewynd.model.ListProgressRequest
import io.rewynd.model.ListProgressResponse
import io.rewynd.model.Progress
import kotlin.time.Clock

fun Route.progressRoutes(db: Database) {
    get("/user/progress/get/{id}") {
        call.parameters["id"]?.let { mediaId ->
            withUsername {
                call.respond(
                    db.getProgress(mediaId, this)?.toProgress() ?: Progress(
                        mediaId,
                        0.0,
                        Clock.System.now(),
                    ),
                )
            }
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
    post("/user/progress/list") {
        withUsername {
            val req: ListProgressRequest = call.receive()
            val res =
                db.listRecentProgress(
                    username = this,
                    cursor = req.cursor,
                    minPercent = req.minProgress,
                    maxPercent = req.maxProgress,
                    limit = req.limit.toInt(),
                ).map { it.toProgress() }
            call.respond(
                ListProgressResponse(results = res, cursor = res.minByOrNull { it.timestamp }?.timestamp),
            )
        }
    }
    post("/user/progress/put") {
        withUsername {
            val req: Progress = call.receive()
            db.upsertProgress(
                UserProgress(
                    username = this,
                    id = req.id,
                    percent = req.percent,
                    timestamp = req.timestamp,
                ),
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}
