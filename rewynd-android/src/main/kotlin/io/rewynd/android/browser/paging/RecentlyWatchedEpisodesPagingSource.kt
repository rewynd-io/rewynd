package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.rewynd.android.MEDIA_COMPLETED_PERCENT
import io.rewynd.android.MEDIA_STARTED_PERCENT
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.ListStartedEpisodesCursor
import io.rewynd.model.ListStartedEpisodesRequest

class RecentlyWatchedEpisodesPagingSource(val client: RewyndClient) :
    PagingSource<ListStartedEpisodesCursor, EpisodeInfo>() {
    override fun getRefreshKey(
        state: PagingState<ListStartedEpisodesCursor, EpisodeInfo>
    ): ListStartedEpisodesCursor? = null

    override suspend fun load(params: LoadParams<ListStartedEpisodesCursor>) = client.listStartedEpisodes(
        ListStartedEpisodesRequest(
            minProgress = MEDIA_STARTED_PERCENT,
            maxProgress = MEDIA_COMPLETED_PERCENT,
            cursor = params.key,
        )
    ).result().fold({
        LoadResult.Page(it.page, null, it.cursor)
    }) {
        LoadResult.Error(it)
    }
}
