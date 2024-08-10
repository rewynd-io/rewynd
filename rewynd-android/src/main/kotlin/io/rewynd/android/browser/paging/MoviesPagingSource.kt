package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.ktor.http.HttpStatusCode
import io.rewynd.client.RewyndClient
import io.rewynd.model.ListMoviesRequest
import io.rewynd.model.MovieInfo

class MoviesPagingSource(val libraryId: String, val client: RewyndClient) : PagingSource<String, MovieInfo>() {
    override fun getRefreshKey(state: PagingState<String, MovieInfo>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, MovieInfo> {
        val response = client.listMovies(
            ListMoviesRequest(libraryId, params.key),
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
