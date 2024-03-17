package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.rewynd.common.database.Database

fun Route.seasonRoutes(db: Database) {
    get("/season/list/{showId}") {
        call.parameters["showId"]?.let { showId ->
            call.respond(db.listSeasons(showId).map { it.seasonInfo })
        } ?: call.respond(HttpStatusCode.BadRequest)
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
