package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.ListSeasonsRequest
import io.rewynd.model.SeasonInfo

class SeasonsPagingSource(val showId: String, val client: RewyndClient) : PagingSource<String, SeasonInfo>() {
    override fun getRefreshKey(state: PagingState<String, SeasonInfo>): String? = null

    override suspend fun load(params: LoadParams<String>) = client.listSeasons(
        ListSeasonsRequest(showId, params.key),
    ).result().fold({
        LoadResult.Page(it.page.sortedBy(SeasonInfo::seasonNumber), null, it.cursor)
    }) {
        LoadResult.Error(it)
    }
}
