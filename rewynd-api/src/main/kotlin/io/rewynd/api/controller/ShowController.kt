package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.rewynd.common.database.Database

fun Route.showRoutes(db: Database) {
    get("/show/list/{libraryId}") {
        call.parameters["libraryId"]?.let { libraryId ->
            call.respond(db.listShows(libraryId).map { it.toShowInfo() })
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
    get("/show/get/{showId}") {
        val show = call.parameters["showId"]?.let { db.getShow(it) }?.toShowInfo()
        if (show == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(show)
        }
    }
}
