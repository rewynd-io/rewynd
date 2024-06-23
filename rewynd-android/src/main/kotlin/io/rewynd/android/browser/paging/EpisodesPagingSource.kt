package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.ktor.http.HttpStatusCode
import io.rewynd.client.RewyndClient
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.ListEpisodesRequest

class EpisodesPagingSource(val seasonId: String, val client: RewyndClient) : PagingSource<String, EpisodeInfo>() {
    override fun getRefreshKey(state: PagingState<String, EpisodeInfo>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, EpisodeInfo> {
        val response = client.listEpisodes(
            ListEpisodesRequest(seasonId, params.key),
        )

        return when (response.status) {
            HttpStatusCode.OK.value -> {
                val body = response.body()
                LoadResult.Page(body.page.sortedBy { it.episode }, null, body.cursor)
            }

            else -> {
                LoadResult.Error(HttpStatusCodeException(statusCode = HttpStatusCode.fromValue(response.status)))
            }
        }
    }
}
