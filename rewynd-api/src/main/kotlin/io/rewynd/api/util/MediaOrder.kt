package io.rewynd.api.util

import io.rewynd.common.database.Database
import io.rewynd.common.database.listAllEpisodes
import io.rewynd.common.database.listAllSeasons
import io.rewynd.common.model.ServerEpisodeInfo
import kotlinx.coroutines.flow.toList

suspend fun getFirstEpisodeInNextSeason(
    db: Database,
    serverEpisodeInfo: ServerEpisodeInfo,
    reverse: Boolean = false,
): ServerEpisodeInfo? =
    db.listAllSeasons(serverEpisodeInfo.showId)
        .toList()
        .sortedBy { it.seasonInfo.seasonNumber }
        .let { seasons ->
            val chosenIndex =
                seasons.indexOfFirst {
                    it.seasonInfo.seasonNumber == serverEpisodeInfo.season
                }
            if (chosenIndex == -1) {
                null
            } else {
                seasons.getOrNull(
                    chosenIndex + (if (reverse) -1 else 1),
                )
            }
        }?.let { season -> db.listEpisodes(season.seasonInfo.id) }?.sorted()
        ?.let { if (reverse) it.lastOrNull() else it.firstOrNull() }

suspend fun getNextEpisodeInSeason(
    db: Database,
    serverEpisodeInfo: ServerEpisodeInfo,
    reverse: Boolean = false,
) = db.listAllEpisodes(serverEpisodeInfo.seasonId).toList().sorted().let { seasonEpisodes ->
    val chosenIndex =
        seasonEpisodes.indexOfFirst {
            it.id == serverEpisodeInfo.id
        }
    if (chosenIndex == -1) {
        null
    } else {
        seasonEpisodes.getOrNull(
            chosenIndex + (if (reverse) -1 else 1),
        )
    }
}
