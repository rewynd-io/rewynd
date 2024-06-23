package io.rewynd.android.browser.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import arrow.fx.coroutines.parMapUnordered
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.rewynd.android.MEDIA_COMPLETED_PERCENT
import io.rewynd.android.MEDIA_TOTAL_COMPLETED_PERCENT
import io.rewynd.android.model.Progressed
import io.rewynd.client.RewyndClient
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.GetNextEpisodeRequest
import io.rewynd.model.ListProgressRequest
import io.rewynd.model.NextEpisodeOrder
import io.rewynd.model.Progress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Instant
import kotlin.coroutines.cancellation.CancellationException

class NextEpisodesPagingSource(val client: RewyndClient) : PagingSource<Instant, Progressed<EpisodeInfo>>() {
    override fun getRefreshKey(state: PagingState<Instant, Progressed<EpisodeInfo>>): Instant? = null

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override suspend fun load(params: LoadParams<Instant>): LoadResult<Instant, Progressed<EpisodeInfo>> {
        val progressResponse = client.listProgress(
            ListProgressRequest(
                minPercent = MEDIA_COMPLETED_PERCENT,
                limit = MEDIA_TOTAL_COMPLETED_PERCENT
            ),
        )

        return when (progressResponse.status) {
            HttpStatusCode.OK.value -> {
                val body = progressResponse.body()
                // TODO results should be non-null
                LoadResult.Page(
                    (body.results ?: emptyList()).asFlow().parMapUnordered { getNextEpisode(it) }
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

    private suspend fun getNextEpisode(progress: Progress) = try {
        val res = client.getNextEpisode(GetNextEpisodeRequest(progress.id, NextEpisodeOrder.next))

        when (res.status) {
            HttpStatusCode.OK.value -> {
                res.body().episodeInfo?.let {
                    getNextEpisodeProgress(it)
                }
            }

            else -> null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.warn(e) { "Failed to get next episode" }
        null
    }

    private suspend fun getNextEpisodeProgress(episodeInfo: EpisodeInfo) = try {
        val res = client.getUserProgress(episodeInfo.id)

        when (res.status) {
            HttpStatusCode.OK.value -> {
                Progressed(res.body(), episodeInfo)
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
