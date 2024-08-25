package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.ktor.http.HttpStatusCode
import io.rewynd.client.RewyndClient
import io.rewynd.model.ListUsersRequest
import io.rewynd.model.User

class UserPagingSource(val client: RewyndClient) : PagingSource<String, User>() {
    override fun getRefreshKey(state: PagingState<String, User>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, User> {
        val response = client.listUsers(ListUsersRequest(params.key)) // TODO add page size to request
        when (response.status) {
            HttpStatusCode.OK.value -> {
                val body = response.body()
                return LoadResult.Page(data = body.page.sortedBy { it.username }, prevKey = null, nextKey = body.cursor)
            }
            else -> {
                return LoadResult.Error(HttpStatusCodeException(statusCode = HttpStatusCode.fromValue(response.status)))
            }
        }
    }
}
