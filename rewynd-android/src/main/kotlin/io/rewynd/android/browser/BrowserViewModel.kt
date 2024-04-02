package io.rewynd.android.browser

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.ktor.utils.io.jvm.javaio.copyTo
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.client.RewyndClient
import io.rewynd.client.listEpisodesFlow
import io.rewynd.client.listLibrariesFlow
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Library
import io.rewynd.model.ListEpisodesByLastUpdatedOrder
import io.rewynd.model.ListEpisodesByLastUpdatedRequest
import io.rewynd.model.ListEpisodesRequest
import io.rewynd.model.ListLibrariesRequest
import io.rewynd.model.ListProgressRequest
import io.rewynd.model.Progress
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream

class BrowserViewModel(
    application: Application,
    val serverUrl: ServerUrl,
    private val client: RewyndClient = mkRewyndClient(serverUrl),
) : AndroidViewModel(application) {
    fun popBrowserState() {
        this.browserState.value =
            if (this.browserState.value.size <= 1) {
                listOf(BrowserState.HomeState)
            } else {
                this.browserState.value.dropLast(
                    1,
                )
            }
        loadBrowserState()
    }

    val browserState: MutableStateFlow<List<BrowserState>> =
        MutableStateFlow(emptyList())

    fun initBrowserState(browserState: List<BrowserState>) {
        this.browserState.value = browserState
    }

    private fun loadBrowserState() =
        (browserState.value.lastOrNull() ?: BrowserState.HomeState).let {
            when (it) {
                is BrowserState.HomeState ->
                    runBlocking {
                        listOf(
                            launch(Dispatchers.IO) {
                                loadLibraries()
                            },
                            launch(Dispatchers.IO) {
                                loadLatestEpisodes()
                            },
                            launch(Dispatchers.IO) {
                                loadNewestEpisodes()
                            },
                            launch(Dispatchers.IO) {
                                loadNextEpisodes()
                            },
                        ).joinAll()
                    }

                is BrowserState.LibraryState -> loadShows(it.library.name)
                is BrowserState.ShowState -> loadSeasons(it.showInfo.id)
                is BrowserState.SeasonState -> loadEpisodes(it.seasonInfo.id)
                is BrowserState.EpisodeState -> {
                    loadPreviousEpisode(it.episodeInfo.id)
                    loadNextEpisode(it.episodeInfo.id)
                    loadUserProgress(it.episodeInfo.id)
                }
            }
        }

    fun putBrowserState(browserState: BrowserState) {
        this.browserState.value += listOf(browserState)
        loadBrowserState()
    }

    val libraries = MutableLiveData<List<Library>>(emptyList<Library>())

    fun loadLibraries() {
        Log.i("LibraryLoader", "Loading Libs")

        this.viewModelScope.launch(Dispatchers.IO) {
            client.listLibrariesFlow(ListLibrariesRequest()).collect {
                libraries.postValue((libraries.value ?: emptyList()) + listOf(it))
            }
            Log.i("LibraryLoader", "Loaded ${libraries.value}")
        }
    }

    val latestEpisodes = MutableLiveData<List<EpisodeInfo>>(emptyList<EpisodeInfo>())

    @OptIn(FlowPreview::class)
    fun loadLatestEpisodes() {
        Log.i("LibraryLoader", "Loading Libs")
        this.viewModelScope.launch(Dispatchers.IO) {
            client.listProgress(
                ListProgressRequest(
                    minPercent = 0.05,
                    maxPercent = 0.95,
                    limit = 20.0,
                ),
            ).body().results?.sortedBy { it.timestamp }?.reversed()?.asFlow()?.flatMapMerge {
                kotlin.runCatching { flowOf(it to client.getEpisode(it.id).body()) }.getOrNull()
                    ?: emptyFlow()
            }?.runningFold(emptyList<Pair<Progress, EpisodeInfo>>()) { accumulator, value ->
                accumulator + listOf(value)
            }?.collect { pair ->
                latestEpisodes.postValue(pair.map { it.second })
            }
        }
    }

    val nextEpisodes = MutableLiveData<List<EpisodeInfo>>(emptyList<EpisodeInfo>())

    @OptIn(FlowPreview::class)
    fun loadNextEpisodes() {
        Log.i("LibraryLoader", "Loading Libs")
        this.viewModelScope.launch(Dispatchers.IO) {
            client.listProgress(ListProgressRequest(minPercent = 0.95, limit = 100.0))
                .body()
                .results
                ?.sortedBy { it.timestamp }
                ?.reversed()
                ?.asFlow()
                ?.flatMapMerge {
                    runCatching {
                        val next = client.getNextEpisode(it.id).body()
                        flowOf(client.getUserProgress(next.id).body() to next)
                    }.getOrNull() ?: emptyFlow()
                }?.filter { it.first.percent <= 0.05 }?.take(20)
                ?.runningFold(emptyList<Pair<Progress, EpisodeInfo>>()) { accumulator, value ->
                    accumulator + listOf(value)
                }?.collect { pair ->
                    nextEpisodes.postValue(pair.map { it.second })
                }
        }
    }

    val newestEpisodes = MutableLiveData<List<EpisodeInfo>>(emptyList<EpisodeInfo>())

    @OptIn(FlowPreview::class)
    fun loadNewestEpisodes() {
        this.viewModelScope.launch(Dispatchers.IO) {
            client.listEpisodesByLastUpdated(ListEpisodesByLastUpdatedRequest(order = ListEpisodesByLastUpdatedOrder.Newest))
                .body()
                .episodes
                .let {
                    newestEpisodes.postValue(it)
                }
        }
    }

    val shows = MutableLiveData<List<ShowInfo>>(emptyList<ShowInfo>())

    fun loadShows(libraryName: String) {
        this.viewModelScope.launch(Dispatchers.IO) {
            shows.postValue(
                requireNotNull(
                    client.listShows(libraryName).body().sortedBy { it.title },
                ),
            )
        }
    }

    val seasons = MutableLiveData<List<SeasonInfo>>(emptyList<SeasonInfo>())

    fun loadSeasons(showId: String) {
        this.viewModelScope.launch(Dispatchers.IO) {
            seasons.postValue(
                requireNotNull(
                    client.listSeasons(showId).body().sortedBy { it.seasonNumber },
                ),
            )
        }
    }

    val episodes = MutableLiveData<List<EpisodeInfo>>(emptyList<EpisodeInfo>())

    fun loadEpisodes(seasonId: String) {
        this.viewModelScope.launch(Dispatchers.IO) {
            episodes.postValue(
                client.listEpisodesFlow(ListEpisodesRequest(seasonId)).toList()
            )
        }
    }

    val nextEpisode = MutableLiveData<EpisodeInfo?>(null)

    fun loadNextEpisode(episodeId: String) {
        nextEpisode.postValue(null)
        this.viewModelScope.launch(Dispatchers.IO) {
            nextEpisode.postValue(
                kotlin.runCatching { client.getNextEpisode(episodeId).body() }
                    .getOrNull(),
            )
        }
    }

    val userProgress = MutableLiveData<Progress?>(null)

    fun loadUserProgress(episodeId: String) {
        userProgress.postValue(null)
        this.viewModelScope.launch(Dispatchers.IO) {
            userProgress.postValue(
                kotlin.runCatching { client.getUserProgress(episodeId).body() }
                    .getOrNull()?.takeIf { it.percent < .95 },
            )
        }
    }

    val previousEpisode = MutableLiveData<EpisodeInfo?>(null)

    fun loadPreviousEpisode(episodeId: String) {
        previousEpisode.postValue(null)
        this.viewModelScope.launch(Dispatchers.IO) {
            previousEpisode.postValue(
                kotlin.runCatching { client.getPreviousEpisode(episodeId).body() }
                    .getOrNull(),
            )
        }
    }

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

    companion object {
        const val CACHE_SIZE = 128 * 1024 * 1024 // 4MiB
    }
}
