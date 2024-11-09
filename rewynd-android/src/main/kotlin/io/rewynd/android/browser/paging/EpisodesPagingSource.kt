package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.ListEpisodesRequest
import io.rewynd.model.ListEpisodesRequestOrder
import io.rewynd.model.ListEpisodesRequestOrderProperty
import io.rewynd.model.SortOrder

class EpisodesPagingSource(
    private val seasonId: String,
    val client: RewyndClient
) : PagingSource<String, EpisodeInfo>() {
    override fun getRefreshKey(state: PagingState<String, EpisodeInfo>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, EpisodeInfo> = client.listEpisodes(
        ListEpisodesRequest(
            seasonId = seasonId,
            cursor = params.key,
            order = ListEpisodesRequestOrder(ListEpisodesRequestOrderProperty.EpisodeId, SortOrder.Descending)
        ),
    ).result().fold({
        LoadResult.Page(it.page.sortedBy(EpisodeInfo::episode), null, it.cursor)
    }) {
        LoadResult.Error(it)
    }
}
