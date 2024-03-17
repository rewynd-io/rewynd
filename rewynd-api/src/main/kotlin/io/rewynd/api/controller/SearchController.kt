package io.rewynd.api.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.rewynd.common.cache.queue.SearchJobQueue
import io.rewynd.common.cache.queue.WorkerEvent
import io.rewynd.common.model.SearchProps
import io.rewynd.model.SearchRequest
import io.rewynd.model.SearchResponse
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

fun Route.searchRoutes(queue: SearchJobQueue) {
    post("/search/get") {
        val req = call.receive<SearchRequest>()
        if (req.text.isBlank()) {
            call.respond(SearchResponse(emptyList()))
        } else {
            val jobId = queue.submit(SearchProps(req.text))
            when (val res = queue.monitor(jobId).filter { it.isTerminal() }.first()) {
                is WorkerEvent.Success -> call.respond(Json.decodeFromString<SearchResponse>(res.payload))
                else -> call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
