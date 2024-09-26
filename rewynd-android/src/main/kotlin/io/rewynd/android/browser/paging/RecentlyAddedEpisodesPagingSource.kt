package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.ListEpisodesByLastUpdatedOrder
import io.rewynd.model.ListEpisodesByLastUpdatedRequest

class RecentlyAddedEpisodesPagingSource(val client: RewyndClient) : PagingSource<Long, EpisodeInfo>() {
    override fun getRefreshKey(state: PagingState<Long, EpisodeInfo>): Long? = null

    override suspend fun load(params: LoadParams<Long>) = client.listEpisodesByLastUpdated(
        ListEpisodesByLastUpdatedRequest(
            order = ListEpisodesByLastUpdatedOrder.Newest
        )
    ).result().fold({
        LoadResult.Page(it.episodes, null, it.cursor)
    }) {
        LoadResult.Error(it)
    }
}
