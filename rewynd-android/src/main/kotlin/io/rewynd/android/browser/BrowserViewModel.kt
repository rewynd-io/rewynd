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
import io.rewynd.android.browser.paging.LatestEpisodesPagingSource
import io.rewynd.android.browser.paging.LibraryPagingSource
import io.rewynd.android.browser.paging.NewestEpisodesPagingSource
import io.rewynd.android.browser.paging.NextEpisodesPagingSource
import io.rewynd.android.browser.paging.SeasonsPagingSource
import io.rewynd.android.browser.paging.ShowsPagingSource
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.client.RewyndClient
import io.rewynd.model.Progress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class BrowserViewModel(
    application: Application,
    val serverUrl: ServerUrl,
    private val client: RewyndClient = mkRewyndClient(serverUrl),
) : AndroidViewModel(application) {

    fun getLibraries() = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { LibraryPagingSource(client) }
    ).flow.cachedIn(viewModelScope)

    fun getLatestEpisodes() = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { LatestEpisodesPagingSource(client) }
    ).flow.cachedIn(viewModelScope)

    fun getNextEpisodes() = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { NextEpisodesPagingSource(client) }
    ).flow.cachedIn(viewModelScope)

    fun getNewestEpisodes() = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { NewestEpisodesPagingSource(client) }
    ).flow.cachedIn(viewModelScope)

    fun getShows(libraryName: String) = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { ShowsPagingSource(libraryName, client) }
    ).flow.cachedIn(viewModelScope)

    fun getSeasons(showId: String) = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { SeasonsPagingSource(showId, client) }
    ).flow.cachedIn(viewModelScope)

    fun getEpisodes(seasonId: String) = Pager(
        config = PAGING_CONFIG,
        pagingSourceFactory = { EpisodesPagingSource(seasonId, client) }
    ).flow.cachedIn(viewModelScope)

    private val imageCache = LruCache<String, Bitmap>(CACHE_SIZE)

    suspend fun loadImage(imageId: String): Bitmap? {
        val cached = imageCache.get(imageId)
        return if (cached != null) {
            cached
        } else {
            val retrieved = client.getImage(imageId).body()
            val os = ByteArrayOutputStream()
            val copiedBytes = retrieved.copyTo(os)
            val bitmap =
                BitmapFactory.decodeByteArray(os.toByteArray(), 0, copiedBytes.toInt())?.also {
                    imageCache.put(imageId, it)
                }
            bitmap
        }
    }

    fun getProgress(id: String): StateFlow<Progress?> {
        val state = MutableStateFlow<Progress?>(null)
        this.viewModelScope.launch {
            val res = client.getUserProgress(id)
            when (res.status) {
                HttpStatusCode.OK.value -> {
                    state.emit(res.body())
                }

                else -> state.emit(Progress(id, 0.0, kotlinx.datetime.Instant.fromEpochSeconds(0)))
            }
        }
        return state
    }

    companion object {
        const val CACHE_SIZE = 512 * 1024 * 1024 // 512MiB
        const val LATEST_EPISODES_LIMIT = 20
        val PAGING_CONFIG = PagingConfig(10)
    }
}
