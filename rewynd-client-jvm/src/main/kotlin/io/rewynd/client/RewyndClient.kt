package io.rewynd.client

import io.rewynd.model.ListEpisodesRequest
import io.rewynd.model.ListEpisodesResponse
import io.rewynd.model.ListSchedulesRequest
import io.rewynd.model.ListSchedulesResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.openapitools.client.infrastructure.HttpResponse

typealias RewyndClient = DefaultApi

private fun <Req, Res : Any, Item> mkFlowMethod(
    request: Req,
    operation: suspend (Req) -> HttpResponse<Res>,
    itemGetter: (Res) -> List<Item>,
    repeat: (Res) -> Req?
): Flow<Item> = flow {
    var req: Req? = request
    while (req != null) {
        val res = operation(req)
        check(res.success) { "Operation failed within flow: ${res.status}" }
        val body = res.body()
        emitAll(itemGetter(body).asFlow())
        req = repeat(body)
    }
}

fun RewyndClient.listEpisodesFlow(listEpisodesRequest: ListEpisodesRequest) =
    mkFlowMethod(listEpisodesRequest, this::listEpisodes, ListEpisodesResponse::page) {
        it.cursor?.let { cursor -> listEpisodesRequest.copy(cursor = cursor) }
    }

fun RewyndClient.listSchedulesFlow(listSchedulesRequest: ListSchedulesRequest = ListSchedulesRequest()) =
    mkFlowMethod(listSchedulesRequest, this::listSchedules, ListSchedulesResponse::page) {
        it.cursor?.let { cursor -> listSchedulesRequest.copy(cursor = cursor) }
    }