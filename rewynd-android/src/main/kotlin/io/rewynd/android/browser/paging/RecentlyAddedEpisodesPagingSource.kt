package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.ListEpisodesByLastUpdatedRequest

class RecentlyAddedEpisodesPagingSource(val client: RewyndClient) : PagingSource<String, EpisodeInfo>() {
    override fun getRefreshKey(state: PagingState<String, EpisodeInfo>): String? = null

    override suspend fun load(params: LoadParams<String>) = client.listEpisodesByLastUpdated(
        ListEpisodesByLastUpdatedRequest(
            cursor = params.key
        )
    ).result().fold({
        LoadResult.Page(it.episodes, null, it.cursor)
    }) {
        LoadResult.Error(it)
    }
}
