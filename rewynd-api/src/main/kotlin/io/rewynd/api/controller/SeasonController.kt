package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.rewynd.common.database.Database
import io.rewynd.model.ListSeasonsRequest
import io.rewynd.model.ListSeasonsResponse

fun Route.seasonRoutes(db: Database) {
    post("/season/list") {
        val req = call.receive<ListSeasonsRequest>()
        val page = db.listSeasons(req.showId, req.cursor).map { it.seasonInfo }
        call.respond(ListSeasonsResponse(page, page.lastOrNull()?.id))
    }
    get("/season/get/{seasonId}") {
        val season = call.parameters["seasonId"]?.let { db.getSeason(it) }?.seasonInfo
        if (season == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(season)
        }
    }
}
