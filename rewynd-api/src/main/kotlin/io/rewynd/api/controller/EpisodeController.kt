package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.rewynd.api.util.getFirstEpisodeInNextSeason
import io.rewynd.api.util.getNextEpisodeInSeason
import io.rewynd.common.database.Database
import io.rewynd.common.database.Database.Companion.LIST_EPISODES_MAX_SIZE
import io.rewynd.model.GetNextEpisodeRequest
import io.rewynd.model.GetNextEpisodeResponse
import io.rewynd.model.ListEpisodesByLastUpdatedRequest
import io.rewynd.model.ListEpisodesByLastUpdatedResponse
import io.rewynd.model.ListEpisodesRequest
import io.rewynd.model.ListEpisodesResponse
import io.rewynd.model.NextEpisodeOrder

@Suppress("LongMethod")
fun Route.episodeRoutes(db: Database) {
    post("/episode/list") {
        call.receive<ListEpisodesRequest>().let { request ->
            val page = db.listEpisodes(request.seasonId, request.cursor).map { it.toEpisodeInfo() }
            call.respond(ListEpisodesResponse(page, page.lastOrNull()?.id))
        }
    }
    post("/episode/listByLastUpdated") {
        call.receive<ListEpisodesByLastUpdatedRequest>().let { request ->
            val episodes =
                db.listEpisodesByLastUpdated(
                    request.cursor?.coerceAtLeast(0),
                    request.limit ?: LIST_EPISODES_MAX_SIZE,
                    request.libraryIds,
                    request.order,
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
        db.getEpisode(getNextEpisodeRequest.episodeId)?.let { serverEpisodeInfo ->
            val reverse =
                when (getNextEpisodeRequest.order) {
                    NextEpisodeOrder.previous -> true
                    NextEpisodeOrder.next -> false
                }
            (
                getNextEpisodeInSeason(db, serverEpisodeInfo, reverse) ?: getFirstEpisodeInNextSeason(
                    db,
                    serverEpisodeInfo,
                    reverse,
                )
                )?.let {
                call.respond(GetNextEpisodeResponse(it.toEpisodeInfo()))
            }
        } ?: call.respond(HttpStatusCode.NotFound)
    }
}
