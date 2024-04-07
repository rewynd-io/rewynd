package io.rewynd.worker.scan.show

import io.rewynd.common.model.ServerEpisodeInfo
import io.rewynd.common.model.ServerImageInfo
import io.rewynd.common.model.ServerSeasonInfo
import io.rewynd.common.model.ServerShowInfo

data class ShowScanResults(
    val images: Set<ServerImageInfo> = emptySet(),
    val shows: Set<ServerShowInfo> = emptySet(),
    val seasons: Set<ServerSeasonInfo> = emptySet(),
    val episodes: Set<ServerEpisodeInfo> = emptySet(),
) {
    operator fun plus(other: ShowScanResults) =
        ShowScanResults(
            images = this.images + other.images,
            episodes = this.episodes + other.episodes,
            seasons = this.seasons + other.seasons,
            shows = this.shows + other.shows,
        )

    companion object {
        val EMPTY = ShowScanResults()
    }
}
