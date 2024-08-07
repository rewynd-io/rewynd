package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.ktor.http.HttpStatusCode
import io.rewynd.client.RewyndClient
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.ListEpisodesByLastUpdatedOrder
import io.rewynd.model.ListEpisodesByLastUpdatedRequest

class RecentlyAddedEpisodesPagingSource(val client: RewyndClient) : PagingSource<Long, EpisodeInfo>() {
    override fun getRefreshKey(state: PagingState<Long, EpisodeInfo>): Long? = null

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, EpisodeInfo> {
        val response = client.listEpisodesByLastUpdated(
            ListEpisodesByLastUpdatedRequest(
                order = ListEpisodesByLastUpdatedOrder.Newest
            )
        )

        return when (response.status) {
            HttpStatusCode.OK.value -> {
                val body = response.body()
                LoadResult.Page(body.episodes, null, body.cursor)
            }

            else -> {
                LoadResult.Error(HttpStatusCodeException(statusCode = HttpStatusCode.fromValue(response.status)))
            }
        }
    }
}
