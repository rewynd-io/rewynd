package io.rewynd.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.statement.bodyAsText
import io.rewynd.model.ListEpisodesRequest
import io.rewynd.model.ListEpisodesResponse
import io.rewynd.model.ListLibrariesRequest
import io.rewynd.model.ListLibrariesResponse
import io.rewynd.model.ListSchedulesRequest
import io.rewynd.model.ListSchedulesResponse
import io.rewynd.model.ListSeasonsRequest
import io.rewynd.model.ListSeasonsResponse
import io.rewynd.model.ListShowsRequest
import io.rewynd.model.ListShowsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import net.kensand.kielbasa.coroutines.coRunCatching
import org.openapitools.client.infrastructure.HttpResponse

private val log = KotlinLogging.logger { }

typealias RewyndClient = DefaultApi

private fun <Req, Res : Any, Item> mkFlowMethod(
    request: Req,
    operation: suspend (Req) -> HttpResponse<Res>,
    itemGetter: (Res) -> List<Item>,
    repeat: (Res) -> Req?
): Flow<Result<Item>> = flow {
    var req: Req? = request
    while (req != null) {
        val res = operation(req)
        req = res.result().fold({
            emitAll(itemGetter(it).map(Result.Companion::success).asFlow())
            repeat(it)
        }) {
            log.warn(it) { "Failed to retrieve page" }
            emit(Result.failure(it))
            null
        }
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

fun RewyndClient.listLibrariesFlow(listLibrariesRequest: ListLibrariesRequest) =
    mkFlowMethod(listLibrariesRequest, this::listLibraries, ListLibrariesResponse::page) {
        it.cursor?.let { cursor -> listLibrariesRequest.copy(cursor = cursor) }
    }

fun RewyndClient.listShowsFlow(listShowsRequest: ListShowsRequest) =
    mkFlowMethod(listShowsRequest, this::listShows, ListShowsResponse::page) {
        it.cursor?.let { cursor -> listShowsRequest.copy(cursor = cursor) }
    }

fun RewyndClient.listSeasonsFlow(listSeasonsRequest: ListSeasonsRequest) =
    mkFlowMethod(listSeasonsRequest, this::listSeasons, ListSeasonsResponse::page) {
        it.cursor?.let { cursor -> listSeasonsRequest.copy(cursor = cursor) }
    }

suspend fun <T : Any> HttpResponse<T>.result() = coRunCatching {
    if (success) {
        body()
    } else {
        throw RewyndStatusCodeException(
            this.response.status,
            this.response.bodyAsText()
        ).also { log.warn(it) { "Error calling RewyndApi" } }
    }
}
