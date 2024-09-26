package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.ListUsersRequest
import io.rewynd.model.User

class UserPagingSource(val client: RewyndClient) : PagingSource<String, User>() {
    override fun getRefreshKey(state: PagingState<String, User>): String? = null

    override suspend fun load(params: LoadParams<String>) =
        // TODO add page size to request
        client.listUsers(ListUsersRequest(params.key)).result().fold({
            LoadResult.Page(data = it.page.sortedBy(User::username), prevKey = null, nextKey = it.cursor)
        }) {
            LoadResult.Error(it)
        }
}
