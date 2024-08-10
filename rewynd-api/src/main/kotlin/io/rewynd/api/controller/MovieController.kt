package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.rewynd.common.database.Database
import io.rewynd.model.ListMoviesRequest
import io.rewynd.model.ListMoviesResponse

fun Route.movieRoutes(db: Database) {
    post("/movie/list") {
        val req = call.receive<ListMoviesRequest>()
        val page = db.listMovies(req.libraryId, req.cursor).map { it.toMovieInfo() }
        call.respond(ListMoviesResponse(page, page.lastOrNull()?.id))

        call.parameters["libraryId"]?.let { libraryId ->
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
    get("/movie/get/{movieId}") {
        val show = call.parameters["movieId"]?.let { db.getMovie(it) }?.toMovieInfo()
        if (show == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(show)
        }
    }
}
