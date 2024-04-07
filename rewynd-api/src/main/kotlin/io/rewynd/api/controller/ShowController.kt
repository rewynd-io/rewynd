package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.rewynd.common.database.Database
import io.rewynd.model.ListShowsRequest
import io.rewynd.model.ListShowsResponse

fun Route.showRoutes(db: Database) {
    post("/show/list") {
        val req = call.receive<ListShowsRequest>()
        val page = db.listShows(req.libraryId, req.cursor).map { it.toShowInfo() }
        call.respond(ListShowsResponse(page, page.lastOrNull()?.id))

        call.parameters["libraryId"]?.let { libraryId ->
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
