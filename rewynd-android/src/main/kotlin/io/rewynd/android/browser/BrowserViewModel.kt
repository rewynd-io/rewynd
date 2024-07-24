package io.rewynd.android.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import arrow.fx.coroutines.parMap
import coil3.ImageLoader
import io.ktor.http.HttpStatusCode
import io.rewynd.android.browser.paging.EpisodesPagingSource
import io.rewynd.android.browser.paging.LibraryPagingSource
import io.rewynd.android.browser.paging.NextEpisodesPagingSource
import io.rewynd.android.browser.paging.RecentlyAddedEpisodesPagingSource
import io.rewynd.android.browser.paging.RecentlyWatchedEpisodesPagingSource
import io.rewynd.android.browser.paging.SeasonsPagingSource
import io.rewynd.android.browser.paging.ShowsPagingSource
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.android.image.RewyndClientFetcher
import io.rewynd.android.model.LoadedSearchResult
import io.rewynd.client.RewyndClient
import io.rewynd.model.GetNextEpisodeRequest
import io.rewynd.model.NextEpisodeOrder
import io.rewynd.model.Progress
import io.rewynd.model.SearchRequest
import io.rewynd.model.SearchResultType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.openapitools.client.infrastructure.HttpResponse

@OptIn(FlowPreview::class)
class BrowserViewModel(
    application: Application,
    val serverUrl: ServerUrl,
    private val client: RewyndClient = mkRewyndClient(serverUrl),
    val imageLoader: ImageLoader =
        ImageLoader
            .Builder(application)
            .components {
                add(RewyndClientFetcher.Factory(client))
            }.build(),
) : AndroidViewModel(application) {
    fun listLibraries() =
        Pager(
            config = PAGING_CONFIG,
            pagingSourceFactory = { LibraryPagingSource(client) },
        ).flow.cachedIn(viewModelScope)

    fun listRecentlyWatchedEpisodes() =
        Pager(
            config = PAGING_CONFIG,
            pagingSourceFactory = { RecentlyWatchedEpisodesPagingSource(client) },
        ).flow.cachedIn(viewModelScope)

    fun listNextEpisodes() =
        Pager(
            config = PAGING_CONFIG,
            pagingSourceFactory = { NextEpisodesPagingSource(client) },
        ).flow.cachedIn(viewModelScope)

    fun listRecentlyAddedEpisodes() =
        Pager(
            config = PAGING_CONFIG,
            pagingSourceFactory = { RecentlyAddedEpisodesPagingSource(client) },
        ).flow.cachedIn(viewModelScope)

    fun listShows(libraryName: String) =
        Pager(
            config = PAGING_CONFIG,
            pagingSourceFactory = { ShowsPagingSource(libraryName, client) },
        ).flow.cachedIn(viewModelScope)

    fun listSeasons(showId: String) =
        Pager(
            config = PAGING_CONFIG,
            pagingSourceFactory = { SeasonsPagingSource(showId, client) },
        ).flow.cachedIn(viewModelScope)

    fun listEpisodes(seasonId: String) =
        Pager(
            config = PAGING_CONFIG,
            pagingSourceFactory = { EpisodesPagingSource(seasonId, client) },
        ).flow.cachedIn(viewModelScope)

    fun getProgress(id: String) =
        getState(
            id,
            client::getUserProgress,
        ).map { it ?: Progress(id, 0.0, kotlinx.datetime.Instant.fromEpochSeconds(0)) }

    fun getEpisode(id: String) = getState(id, client::getEpisode)

    fun getSeason(id: String) = getState(id, client::getSeasons)

    fun getShow(id: String) = getState(id, client::getShow)

    fun getLibrary(id: String) = getState(id, client::getLibrary)

    fun getNextEpisode(id: String) =
        getState(GetNextEpisodeRequest(id, NextEpisodeOrder.next), client::getNextEpisode).map { it?.episodeInfo }

    fun getPrevEpisode(id: String) =
        getState(GetNextEpisodeRequest(id, NextEpisodeOrder.previous), client::getNextEpisode).map { it?.episodeInfo }

    private val _searchText = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<LoadedSearchResult>>(emptyList())

    val searchText
        get() = _searchText.asStateFlow()
    val searchResults
        get() = _searchResults.asStateFlow()

    fun search(query: String) {
        _searchText.value = query
    }

    init {
        viewModelScope.launch {
            searchText.collectLatest { query ->
                client.search(SearchRequest(query)).body().let { res ->
                    _searchResults.value =
                        res.results
                            .asFlow()
                            .parMap {
                                when (it.resultType) {
                                    SearchResultType.Episode ->
                                        LoadedSearchResult.Episode(
                                            it,
                                            client.getEpisode(it.id).body(),
                                        )

                                    SearchResultType.Season ->
                                        LoadedSearchResult.Season(
                                            it,
                                            client.getSeasons(it.id).body(),
                                        )

                                    SearchResultType.Show -> LoadedSearchResult.Show(it, client.getShow(it.id).body())
                                    SearchResultType.Movie -> null // TODO Support Movies
                                }
                            }.filterNotNull()
                            .toList()
                }
            }
        }
    }

    companion object {
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
