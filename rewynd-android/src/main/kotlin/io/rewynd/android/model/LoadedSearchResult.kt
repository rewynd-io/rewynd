package io.rewynd.android.model

import io.rewynd.model.EpisodeInfo
import io.rewynd.model.SearchResult
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo

sealed interface LoadedSearchResult {
    val result: SearchResult

    data class Episode(
        override val result: SearchResult,
        val media: EpisodeInfo
    ) : LoadedSearchResult

    data class Season(
        override val result: SearchResult,
        val media: SeasonInfo
    ) : LoadedSearchResult

    data class Show(
        override val result: SearchResult,
        val media: ShowInfo
    ) : LoadedSearchResult
}
