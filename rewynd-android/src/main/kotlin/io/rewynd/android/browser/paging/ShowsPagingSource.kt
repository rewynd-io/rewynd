package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.ktor.http.HttpStatusCode
import io.rewynd.client.RewyndClient
import io.rewynd.model.ListShowsRequest
import io.rewynd.model.ShowInfo

class ShowsPagingSource(val libraryId: String, val client: RewyndClient) : PagingSource<String, ShowInfo>() {
    override fun getRefreshKey(state: PagingState<String, ShowInfo>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ShowInfo> {
        val response = client.listShows(
            ListShowsRequest(libraryId, params.key),
        )

        return when (response.status) {
            HttpStatusCode.OK.value -> {
                val body = response.body()
                LoadResult.Page(body.page.sortedBy { it.title }, null, body.cursor)
            }

            else -> {
                LoadResult.Error(HttpStatusCodeException(statusCode = HttpStatusCode.fromValue(response.status)))
            }
        }
    }
}
