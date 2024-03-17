package io.rewynd.api.util

import io.rewynd.common.database.Database
import io.rewynd.common.model.ServerEpisodeInfo
import java.text.Collator
import java.util.Comparator
import java.util.Locale

suspend fun getFirstEpisodeInNextSeason(
    db: Database,
    serverEpisodeInfo: ServerEpisodeInfo,
    reverse: Boolean = false,
): ServerEpisodeInfo? =
    db.listSeasons(serverEpisodeInfo.showId)
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
        }?.let { season -> db.listEpisodes(season.seasonInfo.id) }?.sort()
        ?.let { if (reverse) it.lastOrNull() else it.firstOrNull() }

suspend fun getNextEpisodeInSeason(
    db: Database,
    serverEpisodeInfo: ServerEpisodeInfo,
    reverse: Boolean = false,
) = db.listEpisodes(serverEpisodeInfo.seasonId).sort().let { seasonEpisodes ->
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

fun List<ServerEpisodeInfo>.sort() =
    if (this.all { it.episode != null }) {
        this.sortedBy { it.episode }
    } else {
        // TODO take Locale as an argument
        this.sortedWith(
            ServerEpisodeInfoComparator(),
        )
    }

class ServerEpisodeInfoComparator(locale: Locale = Locale.US) : Comparator<ServerEpisodeInfo> {
    private val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }

    override fun compare(
        p0: ServerEpisodeInfo,
        p1: ServerEpisodeInfo,
    ): Int = collator.compare(p0.title, p1.title)
}
