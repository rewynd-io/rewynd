package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.rewynd.api.UserSession.Companion.withUsername
import io.rewynd.common.database.Database
import io.rewynd.common.model.toMovieInfo
import io.rewynd.model.ListMoviesRequest
import io.rewynd.model.ListMoviesResponse

fun Route.movieRoutes(db: Database) {
    post("/movie/list") {
        withUsername {
            val req = call.receive<ListMoviesRequest>()
            val page = db.listProgressedMovies(req.libraryId, req.cursor, this)
            call.respond(ListMoviesResponse(page.data.map { it.toMovieInfo() }, page.cursor))

            call.parameters["libraryId"]?.let { libraryId ->
            } ?: call.respond(HttpStatusCode.BadRequest)
        }
    }
    get("/movie/get/{movieId}") {
        withUsername {
            val show = call.parameters["movieId"]?.let { db.getProgressedMovie(it, this) }?.toMovieInfo()
            if (show == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(show)
            }
        }
    }
}
