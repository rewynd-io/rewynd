package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.rewynd.android.MEDIA_COMPLETED_PERCENT
import io.rewynd.android.MEDIA_STARTED_PERCENT
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.ListEpisodesRequest
import io.rewynd.model.ListEpisodesRequestOrder
import io.rewynd.model.ListEpisodesRequestOrderProperty
import io.rewynd.model.SortOrder

class RecentlyWatchedEpisodesPagingSource(val client: RewyndClient) : PagingSource<String, EpisodeInfo>() {
    override fun getRefreshKey(state: PagingState<String, EpisodeInfo>): String? = null

    override suspend fun load(params: LoadParams<String>) = client.listEpisodes(
        ListEpisodesRequest(
            minProgress = MEDIA_STARTED_PERCENT,
            maxProgress = MEDIA_COMPLETED_PERCENT,
            cursor = params.key,
            order = ListEpisodesRequestOrder(ListEpisodesRequestOrderProperty.ProgressTimestamp, SortOrder.Descending)
        )
    ).result().fold({
        LoadResult.Page(it.page, null, it.cursor)
    }) {
        LoadResult.Error(it)
    }
}
