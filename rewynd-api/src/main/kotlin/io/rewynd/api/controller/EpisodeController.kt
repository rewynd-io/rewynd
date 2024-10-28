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
import io.rewynd.common.model.toEpisodeInfo
import io.rewynd.model.GetNextEpisodeRequest
import io.rewynd.model.GetNextEpisodeResponse
import io.rewynd.model.ListEpisodesRequest
import io.rewynd.model.ListEpisodesResponse
import io.rewynd.model.ListNextEpisodesRequest
import io.rewynd.model.ListNextEpisodesResponse

@Suppress("LongMethod")
fun Route.episodeRoutes(db: Database) {
    post("/episode/list") {
        withUsername {
            call.receive<ListEpisodesRequest>().let { request ->
                val page = db.listProgressedEpisodes(
                    username = this,
                    cursor = request.cursor,
                    seasonId = request.seasonId,
                    minProgress = request.minProgress,
                    maxProgress = request.maxProgress,
                    order = request.order
                )
                call.respond(ListEpisodesResponse(page.data.map { it.toEpisodeInfo() }, page.cursor))
            }
        }
    }

    post("/episode/listNext") {
        withUsername {
            call.receive<ListNextEpisodesRequest>().let { request ->
                val page = db.listNextEpisodes(request.cursor?.toLongOrNull(), this)
                call.respond(ListNextEpisodesResponse(page.data.map { it.toEpisodeInfo() }, page.cursor?.toString()))
            }
        }
    }

    get("/episode/get/{episodeId}") {
        withUsername {
            val episode = call.parameters["episodeId"]?.let { db.getProgressedEpisode(it, this) }?.toEpisodeInfo()
            if (episode == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(episode)
            }
        }
    }

    post("/episode/next") {
        val req = call.receive<GetNextEpisodeRequest>()
        withUsername {
            db.getNextProgressedEpisode(req.episodeId, req.sortOrder, this).let {
                call.respond(GetNextEpisodeResponse(it?.toEpisodeInfo()))
            }
        }
    }
}
