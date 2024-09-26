package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.rewynd.common.database.Database
import io.rewynd.model.GetNextEpisodeRequest
import io.rewynd.model.GetNextEpisodeResponse
import io.rewynd.model.ListEpisodesByLastUpdatedRequest
import io.rewynd.model.ListEpisodesByLastUpdatedResponse
import io.rewynd.model.ListEpisodesRequest
import io.rewynd.model.ListEpisodesResponse

@Suppress("LongMethod")
fun Route.episodeRoutes(db: Database) {
    post("/episode/list") {
        call.receive<ListEpisodesRequest>().let { request ->
            val page = db.listEpisodes(request.seasonId, request.cursor)
            call.respond(ListEpisodesResponse(page.data.map { it.toEpisodeInfo() }, page.cursor))
        }
    }
    post("/episode/listByLastUpdated") {
        call.receive<ListEpisodesByLastUpdatedRequest>().let { request ->
            val episodes =
                db.listEpisodesByLastUpdated(
                    request.cursor,
                    request.limit,
                    request.libraryIds,
                )
            val res =
                ListEpisodesByLastUpdatedResponse(
                    episodes.data.map { it.toEpisodeInfo() },
                    episodes.cursor,
                )
            call.respond(HttpStatusCode.OK, res)
        }
    }
    get("/episode/get/{episodeId}") {
        val episode = call.parameters["episodeId"]?.let { db.getEpisode(it) }?.toEpisodeInfo()
        if (episode == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(episode)
        }
    }

    post("/episode/next") {
        val getNextEpisodeRequest = call.receive<GetNextEpisodeRequest>()
        val next = db.getNextEpisode(getNextEpisodeRequest.episodeId, getNextEpisodeRequest.sortOrder)
        call.respond(GetNextEpisodeResponse(next?.toEpisodeInfo()))
    }
}
