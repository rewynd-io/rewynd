package io.rewynd.common.database

import io.rewynd.common.model.ServerEpisodeInfo
import io.rewynd.common.model.ServerSeasonInfo
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

private suspend fun <Item, Cursor> iterateWithCursor(
    call: suspend (Cursor?) -> List<Item>,
    cursorProp: List<Item>.() -> Cursor?,
) = flow {
    var cursor: Cursor? = null
    do {
        val res = call.invoke(cursor)
        emitAll(res.asFlow())
        cursor = res.cursorProp()
    } while (cursor != null)
}

private suspend fun <Item, Cursor> iterateWithCursor(
    call: suspend (Cursor?) -> Paged<Item, Cursor>,
) = flow {
    var cursor: Cursor? = null
    do {
        val res = call.invoke(cursor)
        emitAll(res.data.asFlow())
        cursor = res.cursor
    } while (cursor != null)
}

suspend fun Database.listAllEpisodes(seasonId: String) =
    iterateWithCursor<ServerEpisodeInfo, String> { listEpisodes(seasonId, it) }

suspend fun Database.listAllSeasons(showId: String) =
    iterateWithCursor<ServerSeasonInfo, String>({ listSeasons(showId, it) }) { lastOrNull()?.seasonInfo?.id }

suspend fun Database.listAllSchedules() = iterateWithCursor(this::listSchedules) { lastOrNull()?.id }

suspend fun Database.listAllLibraries() = iterateWithCursor(this::listLibraries) { lastOrNull()?.name }

suspend fun Database.listAllUsers() = iterateWithCursor(this::listUsers) { lastOrNull()?.user?.username }
