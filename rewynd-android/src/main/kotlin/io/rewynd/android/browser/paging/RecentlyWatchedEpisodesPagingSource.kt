package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.fx.coroutines.parMapUnordered
import io.rewynd.android.MEDIA_COMPLETED_PERCENT
import io.rewynd.android.MEDIA_STARTED_PERCENT
import io.rewynd.android.model.Progressed
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.ListProgressRequest
import io.rewynd.model.Progress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Instant

class RecentlyWatchedEpisodesPagingSource(val client: RewyndClient) : PagingSource<Instant, Progressed<EpisodeInfo>>() {
    override fun getRefreshKey(state: PagingState<Instant, Progressed<EpisodeInfo>>): Instant? = null

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override suspend fun load(params: LoadParams<Instant>) = client.listProgress(
        ListProgressRequest(
            minPercent = MEDIA_STARTED_PERCENT,
            maxPercent = MEDIA_COMPLETED_PERCENT,
            cursor = params.key,
            limit = params.loadSize.toDouble(),
        ),
    ).result().fold(
        {
            LoadResult.Page(
                (it.results ?: emptyList()).asFlow().parMapUnordered { getEpisode(it) }
                    .filterNotNull().toList()
                    .sortedBy { progress -> progress.progress.timestamp }.reversed(),
                null,
                it.cursor
            )
        }
    ) {
        LoadResult.Error(it)
    }

    private suspend fun getEpisode(progress: Progress) =
        client.getEpisode(progress.id).result().getOrNull()?.let { Progressed(progress, it) }
}
