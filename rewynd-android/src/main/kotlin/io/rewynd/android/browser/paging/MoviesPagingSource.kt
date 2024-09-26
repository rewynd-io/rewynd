package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.ListMoviesRequest
import io.rewynd.model.MovieInfo

class MoviesPagingSource(private val libraryId: String, val client: RewyndClient) : PagingSource<String, MovieInfo>() {
    override fun getRefreshKey(state: PagingState<String, MovieInfo>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, MovieInfo> = client.listMovies(
        ListMoviesRequest(libraryId, params.key),
    ).result().fold({
        LoadResult.Page(it.page.sortedBy(MovieInfo::title), null, it.cursor)
    }) {
        LoadResult.Error(it)
    }
}
