package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.ktor.http.HttpStatusCode
import io.rewynd.client.RewyndClient
import io.rewynd.model.Library
import io.rewynd.model.ListLibrariesRequest

class LibraryPagingSource(val client: RewyndClient) : PagingSource<String, Library>() {
    override fun getRefreshKey(state: PagingState<String, Library>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Library> {
        val response = client.listLibraries(ListLibrariesRequest(params.key)) // TODO add page size to request
        when (response.status) {
            HttpStatusCode.OK.value -> {
                val body = response.body()
                return LoadResult.Page(data = body.page.sortedBy { it.name }, prevKey = null, nextKey = body.cursor)
            }
            else -> {
                return LoadResult.Error(HttpStatusCodeException(statusCode = HttpStatusCode.fromValue(response.status)))
            }
        }
    }
}
