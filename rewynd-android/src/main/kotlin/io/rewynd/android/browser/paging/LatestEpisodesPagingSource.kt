package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.fx.coroutines.parMapUnordered
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.rewynd.android.MEDIA_COMPLETED_PERCENT
import io.rewynd.android.MEDIA_STARTED_PERCENT
import io.rewynd.android.browser.BrowserViewModel.Companion.LATEST_EPISODES_LIMIT
import io.rewynd.android.model.Progressed
import io.rewynd.client.RewyndClient
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.ListProgressRequest
import io.rewynd.model.Progress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Instant
import kotlin.coroutines.cancellation.CancellationException

class LatestEpisodesPagingSource(val client: RewyndClient) : PagingSource<Instant, Progressed<EpisodeInfo>>() {
    override fun getRefreshKey(state: PagingState<Instant, Progressed<EpisodeInfo>>): Instant? = null

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override suspend fun load(params: LoadParams<Instant>): LoadResult<Instant, Progressed<EpisodeInfo>> {
        val progressResponse = client.listProgress(
            ListProgressRequest(
                minPercent = MEDIA_STARTED_PERCENT,
                maxPercent = MEDIA_COMPLETED_PERCENT,
                limit = LATEST_EPISODES_LIMIT.toDouble(),
            ),
        )

        return when (progressResponse.status) {
            HttpStatusCode.OK.value -> {
                val body = progressResponse.body()
                // TODO results should be non-null
                LoadResult.Page(
                    (body.results ?: emptyList()).asFlow().parMapUnordered { getEpisode(it) }
                        .filterNotNull().toList()
                        .sortedBy { it.progress.timestamp }.reversed(),
                    null,
                    body.cursor
                )
            }

            else -> {
                LoadResult.Error(
                    HttpStatusCodeException(statusCode = HttpStatusCode.fromValue(progressResponse.status))
                )
            }
        }
    }

    private suspend fun getEpisode(progress: Progress) = try {
        val res = client.getEpisode(progress.id)
        when (res.status) {
            HttpStatusCode.OK.value -> {
                Progressed(progress, res.body())
            }

            else -> null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.warn(e) { "Failed to get next episode" }
        null
    }
    companion object {
        private val log = KotlinLogging.logger { }
    }
}
