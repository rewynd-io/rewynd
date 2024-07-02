package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.rewynd.api.UserSession
import io.rewynd.common.database.Database
import io.rewynd.common.model.UserProgress
import io.rewynd.model.ListProgressRequest
import io.rewynd.model.ListProgressResponse
import io.rewynd.model.Progress
import kotlinx.datetime.Clock

fun Route.progressRoutes(db: Database) {
    get("/user/progress/get/{id}") {
        call.parameters["id"]?.let { mediaId ->
            call.sessions.get<UserSession>()?.username?.let { username ->
                call.respond(
                    db.getProgress(mediaId, username)?.toProgress() ?: Progress(
                        mediaId,
                        0.0,
                        Clock.System.now(),
                    ),
                )
            } ?: call.respond(HttpStatusCode.Forbidden)
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
    post("/user/progress/list") {
        call.sessions.get<UserSession>()?.username?.let { username ->
            val req: ListProgressRequest = call.receive()
            val res =
                db.listRecentProgress(
                    username = username,
                    cursor = req.cursor,
                    minPercent = req.minPercent ?: Database.LIST_PROGRESS_MIN_PERCENT,
                    maxPercent = req.maxPercent ?: Database.LIST_PROGRESS_MAX_PERCENT,
                    limit = req.limit?.toInt() ?: Database.LIST_PROGRESS_MAX_SIZE,
                ).map { it.toProgress() }
            call.respond(
                ListProgressResponse(results = res, cursor = res.minByOrNull { it.timestamp }?.timestamp),
            )
        } ?: call.respond(HttpStatusCode.Forbidden)
    }
    post("/user/progress/put") {
        call.sessions.get<UserSession>()?.username?.let { username ->
            val req: Progress = call.receive()
            db.upsertProgress(
                UserProgress(
                    username,
                    id = req.id,
                    percent = req.percent,
                    timestamp = req.timestamp,
                ),
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}
