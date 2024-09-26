package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.ListShowsRequest
import io.rewynd.model.ShowInfo

class ShowsPagingSource(val libraryId: String, val client: RewyndClient) : PagingSource<String, ShowInfo>() {
    override fun getRefreshKey(state: PagingState<String, ShowInfo>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ShowInfo> = client.listShows(
        ListShowsRequest(libraryId, params.key),
    ).result().fold({
        LoadResult.Page(it.page.sortedBy(ShowInfo::title), null, it.cursor)
    }) {
        LoadResult.Error(it)
    }
}
