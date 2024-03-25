package io.rewynd.client

import io.rewynd.model.ListEpisodesRequest
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

typealias RewyndClient = DefaultApi

fun RewyndClient.listEpisodesFlow(listEpisodesRequest: ListEpisodesRequest) = flow {
    var req = listEpisodesRequest
    do {
        val res = listEpisodes(req)
        check(res.success) { "listEpisodes call failed"}
        val body = res.body()
        val req = listEpisodesRequest.copy(cursor = body.cursor)
        emitAll(body.page.asFlow())
    } while (req.cursor != null)
}