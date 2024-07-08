package io.rewynd.android.browser

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.jvm.javaio.copyTo
import io.rewynd.android.browser.paging.EpisodesPagingSource
import io.rewynd.android.browser.paging.LibraryPagingSource
import io.rewynd.android.browser.paging.NextEpisodesPagingSource
import io.rewynd.android.browser.paging.RecentlyAddedEpisodesPagingSource
import io.rewynd.android.browser.paging.RecentlyWatchedEpisodesPagingSource
import io.rewynd.android.browser.paging.SeasonsPagingSource
import io.rewynd.android.browser.paging.ShowsPagingSource
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.client.RewyndClient
import io.rewynd.model.GetNextEpisodeRequest
import io.rewynd.model.NextEpisodeOrder
import io.rewynd.model.Progress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.openapitools.client.infrastructure.HttpResponse
import java.io.ByteArrayOutputStream

class BrowserViewModel(
    application: Application,
    val serverUrl: ServerUrl,
    private val client: RewyndClient = mkRewyndClient(serverUrl),
) : AndroidViewModel(application) {

    fun listLibraries() = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { LibraryPagingSource(client) }
    ).flow.cachedIn(viewModelScope)

    fun listRecentlyWatchedEpisodes() = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { RecentlyWatchedEpisodesPagingSource(client) }
    ).flow.cachedIn(viewModelScope)

    fun listNextEpisodes() = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { NextEpisodesPagingSource(client) }
    ).flow.cachedIn(viewModelScope)

    fun listRecentlyAddedEpisodes() = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { RecentlyAddedEpisodesPagingSource(client) }
    ).flow.cachedIn(viewModelScope)

    fun listShows(libraryName: String) = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { ShowsPagingSource(libraryName, client) }
    ).flow.cachedIn(viewModelScope)

    fun listSeasons(showId: String) = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { SeasonsPagingSource(showId, client) }
    ).flow.cachedIn(viewModelScope)

    fun listEpisodes(seasonId: String) = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { EpisodesPagingSource(seasonId, client) }
    ).flow.cachedIn(viewModelScope)

    private val imageCache = LruCache<String, Bitmap>(CACHE_SIZE)

    fun loadImage(imageId: String): Flow<Bitmap?> {
        val cached = imageCache.get(imageId)
        return if (cached != null) {
            flowOf(cached)
        } else {
            getState(imageId, client::getImage).map { retrieved ->
                retrieved?.let {
                    val os = ByteArrayOutputStream()
                    val copiedBytes = retrieved.copyTo(os)
                    val bitmap = BitmapFactory.decodeByteArray(os.toByteArray(), 0, copiedBytes.toInt())
                    bitmap
                }
            }.onEach { imageCache.put(imageId, it) }
        }
    }

    fun getProgress(id: String) = getState(
        id,
        client::getUserProgress,

    ).map { it ?: Progress(id, 0.0, kotlinx.datetime.Instant.fromEpochSeconds(0)) }

    fun getSeason(id: String) = getState(id, client::getSeasons)
    fun getShow(id: String) = getState(id, client::getShow)
    fun getLibrary(id: String) = getState(id, client::getLibrary)
    fun getNextEpisode(
        id: String
    ) = getState(GetNextEpisodeRequest(id, NextEpisodeOrder.next), client::getNextEpisode).map { it?.episodeInfo }
    fun getPrevEpisode(
        id: String
    ) = getState(GetNextEpisodeRequest(id, NextEpisodeOrder.previous), client::getNextEpisode).map { it?.episodeInfo }

    companion object {
        const val CACHE_SIZE = 512 * 1024 * 1024 // 512MiB
        const val LATEST_EPISODES_LIMIT = 20
        val PAGING_CONFIG = PagingConfig(10)
    }
}

fun <Id, Response : Any> getState(
    id: Id,
    func: suspend (Id) -> HttpResponse<Response>,
) = flow {
    val res = func(id)
    when (res.status) {
        HttpStatusCode.OK.value -> {
            emit(res.body())
        }

        else -> emit(null)
    }
}.flowOn(Dispatchers.IO)
