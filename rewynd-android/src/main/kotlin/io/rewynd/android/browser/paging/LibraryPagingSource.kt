package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.Library
import io.rewynd.model.ListLibrariesRequest

class LibraryPagingSource(val client: RewyndClient) : PagingSource<String, Library>() {
    override fun getRefreshKey(state: PagingState<String, Library>): String? = null

    // TODO add page size to request
    override suspend fun load(params: LoadParams<String>): LoadResult<String, Library> =
        client.listLibraries(ListLibrariesRequest(params.key))
            .result().fold({
                LoadResult.Page(it.page.sortedBy(Library::name), null, it.cursor)
            }) {
                LoadResult.Error(it)
            }
}
