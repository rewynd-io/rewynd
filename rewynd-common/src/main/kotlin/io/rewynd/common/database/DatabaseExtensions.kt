package io.rewynd.common.database

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

suspend fun Database.listAllEpisodes(seasonId: String) =
    flow {
        var cursor: String? = null
        do {
            val res = listEpisodes(seasonId, cursor = cursor)
            emitAll(res.asFlow())
            cursor = res.lastOrNull()?.id
        } while (cursor != null)
    }

suspend fun Database.listAllSchedules() =
    flow {
        var cursor: String? = null
        do {
            val res = listSchedules(cursor = cursor)
            emitAll(res.asFlow())
            cursor = res.lastOrNull()?.id
        } while (cursor != null)
    }
