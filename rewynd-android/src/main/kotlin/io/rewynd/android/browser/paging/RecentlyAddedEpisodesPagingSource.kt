package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.rewynd.android.MEDIA_COMPLETED_PERCENT
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.ListNewEpisodesCursor
import io.rewynd.model.ListNewEpisodesRequest

class RecentlyAddedEpisodesPagingSource(val client: RewyndClient) : PagingSource<ListNewEpisodesCursor, EpisodeInfo>() {
    override fun getRefreshKey(state: PagingState<ListNewEpisodesCursor, EpisodeInfo>): ListNewEpisodesCursor? = null

    override suspend fun load(params: LoadParams<ListNewEpisodesCursor>) = client.listNewEpisodes(
        ListNewEpisodesRequest(
            maxProgress = MEDIA_COMPLETED_PERCENT,
            cursor = params.key,
        )
    ).result().fold({
        LoadResult.Page(it.page, null, it.cursor)
    }) {
        LoadResult.Error(it)
    }
}
