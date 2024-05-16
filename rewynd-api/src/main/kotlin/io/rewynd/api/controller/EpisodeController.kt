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
import io.rewynd.model.GetNextEpisodeRequest
import io.rewynd.model.GetNextEpisodeResponse
import io.rewynd.model.ListEpisodesByLastUpdatedRequest
import io.rewynd.model.ListEpisodesByLastUpdatedResponse
import io.rewynd.model.ListEpisodesRequest
import io.rewynd.model.ListEpisodesResponse
import io.rewynd.model.NextEpisodeOrder
import kotlinx.datetime.Instant

fun Route.episodeRoutes(db: Database) {
    post("/episode/list") {
        call.receive<ListEpisodesRequest>().let { request ->
            val page = db.listEpisodes(request.seasonId, request.cursor).map { it.toEpisodeInfo() }
            call.respond(ListEpisodesResponse(page, page.lastOrNull()?.id))
        }
    }
    post("/episode/listByLastUpdated") {
        call.receive<ListEpisodesByLastUpdatedRequest>().let { request ->
            try {
                val cursor = request.cursor?.let { it1 -> Instant.parse(it1) }
                val episodes =
                    db.listEpisodesByLastUpdated(
                        cursor?.toEpochMilliseconds(),
                        request.libraryIds,
                        request.order,
                    )
                val res =
                    ListEpisodesByLastUpdatedResponse(
                        cursor =
                            episodes.maxByOrNull { it.lastUpdated }
                                ?.lastUpdated
                                ?.toString(),
                        episodes = episodes.map { it.toEpisodeInfo() },
                    )
                call.respond(HttpStatusCode.OK, res)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid cursor: ${request.cursor}")
            }
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
