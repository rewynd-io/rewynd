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
import io.rewynd.model.ListEpisodesByLastUpdatedRequest
import io.rewynd.model.ListEpisodesByLastUpdatedResponse

fun Route.episodeRoutes(db: Database) {
    get("/episode/list/{seasonId}") {
        call.parameters["seasonId"]?.let { seasonId ->
            call.respond(db.listEpisodes(seasonId).map { it.toEpisodeInfo() })
        } ?: call.respond(HttpStatusCode.BadRequest)
    }
    post("/episode/listByLastUpdated") {
        call.receive<ListEpisodesByLastUpdatedRequest>().let { request ->
            try {
                val cursor = request.cursor?.toLong()
                db.listEpisodesByLastUpdated(cursor, request.order).let { episodes ->
                    ListEpisodesByLastUpdatedResponse(
                        cursor = episodes.lastOrNull()?.lastUpdated?.toEpochMilliseconds()?.toString(),
                        episodes = episodes.map { it.toEpisodeInfo() },
                    ).let { call.respond(HttpStatusCode.OK, it) }
                }
            } catch (e: NumberFormatException) {
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

    // TODO actually implement these
    get("/episode/next/{episodeId}") {
        call.parameters["episodeId"]?.let { db.getEpisode(it) }?.let { serverEpisodeInfo ->
            (getNextEpisodeInSeason(db, serverEpisodeInfo) ?: getFirstEpisodeInNextSeason(db, serverEpisodeInfo))?.let {
                call.respond(it.toEpisodeInfo())
            }
        } ?: call.respond(HttpStatusCode.NotFound)
    }
    get("/episode/previous/{episodeId}") {
        call.parameters["episodeId"]?.let { db.getEpisode(it) }?.let { serverEpisodeInfo ->
            (
                getNextEpisodeInSeason(db, serverEpisodeInfo, true) ?: getFirstEpisodeInNextSeason(
                    db,
                    serverEpisodeInfo,
                    true,
                )
            )?.let {
                call.respond(it.toEpisodeInfo())
            }
        } ?: call.respond(HttpStatusCode.NotFound)
    }
}
