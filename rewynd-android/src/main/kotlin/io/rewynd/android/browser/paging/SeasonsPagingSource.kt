package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.ktor.http.HttpStatusCode
import io.rewynd.client.RewyndClient
import io.rewynd.model.ListSeasonsRequest
import io.rewynd.model.SeasonInfo

class SeasonsPagingSource(val showId: String, val client: RewyndClient) : PagingSource<String, SeasonInfo>() {
    override fun getRefreshKey(state: PagingState<String, SeasonInfo>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, SeasonInfo> {
        val response = client.listSeasons(
            ListSeasonsRequest(showId, params.key),
        )

        return when (response.status) {
            HttpStatusCode.OK.value -> {
                val body = response.body()
                LoadResult.Page(body.page.sortedBy { it.seasonNumber }, null, body.cursor)
            }

            else -> {
                LoadResult.Error(HttpStatusCodeException(statusCode = HttpStatusCode.fromValue(response.status)))
            }
        }
    }
}
