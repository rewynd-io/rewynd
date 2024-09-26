package io.rewynd.android.browser

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import arrow.fx.coroutines.parMap
import coil3.ImageLoader
import io.rewynd.android.browser.paging.EpisodesPagingSource
import io.rewynd.android.browser.paging.LibraryPagingSource
import io.rewynd.android.browser.paging.MoviesPagingSource
import io.rewynd.android.browser.paging.NextEpisodesPagingSource
import io.rewynd.android.browser.paging.RecentlyAddedEpisodesPagingSource
import io.rewynd.android.browser.paging.RecentlyWatchedEpisodesPagingSource
import io.rewynd.android.browser.paging.SeasonsPagingSource
import io.rewynd.android.browser.paging.ShowsPagingSource
import io.rewynd.android.browser.paging.UserPagingSource
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.android.image.RewyndClientFetcher
import io.rewynd.android.model.LoadedSearchResult
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.GetNextEpisodeRequest
import io.rewynd.model.Library
import io.rewynd.model.MovieInfo
import io.rewynd.model.Progress
import io.rewynd.model.SearchRequest
import io.rewynd.model.SearchResultType
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo
import io.rewynd.model.SortOrder
import io.rewynd.model.User
import io.rewynd.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.openapitools.client.infrastructure.HttpResponse

@OptIn(FlowPreview::class)
class BrowserViewModel(
    application: Application,
    private val client: RewyndClient = mkRewyndClient(),
    val imageLoader: ImageLoader =
        ImageLoader
            .Builder(application)
            .components {
                add(RewyndClientFetcher.Factory(client))
            }.build(),
) : AndroidViewModel(application) {

    private val _user = mutableStateOf<User?>(null)
    val user: State<User?>
        get() = _user

    @Composable
    fun loadUser() = LaunchedEffect(_user) {
        if (_user.value == null) {
            _user.value = client.verify().body()
        }
    }

    fun uploadUserPrefs(prefs: UserPreferences) {
        viewModelScope.launch {
            client.putUserPreferences(prefs)
            _user.value = null
        }
    }

    fun listUsers() =
        Pager(
            config = PAGING_CONFIG,
            pagingSourceFactory = { UserPagingSource(client) },
        ).flow.cachedIn(viewModelScope)

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

    fun listMovies(libraryName: String) =
        Pager(
            config = PAGING_CONFIG,
            pagingSourceFactory = { MoviesPagingSource(libraryName, client) },
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

    var progress by mutableStateOf<Progress?>(null)
        private set

    @Composable
    fun loadProgress(id: String) = LaunchedEffect(id) {
        progress = null
        progress = client.getUserProgress(id).body()
            .let { it ?: Progress(id, 0.0, kotlinx.datetime.Instant.fromEpochSeconds(0)) }
    }

    var episode by mutableStateOf<EpisodeInfo?>(null)
        private set

    @Composable
    fun loadEpisode(id: String) = LaunchedEffect(id) {
        episode = null
        episode = client.getEpisode(id).body()
    }

    var nextEpisode by mutableStateOf<EpisodeInfo?>(null)
        private set

    @Composable
    fun loadNextEpisode(id: String) = LaunchedEffect(id) {
        nextEpisode = null
        nextEpisode = client.getNextEpisode(GetNextEpisodeRequest(id, SortOrder.Ascending)).body().episodeInfo
    }

    var prevEpisode by mutableStateOf<EpisodeInfo?>(null)
        private set

    @Composable
    fun loadPrevEpisode(id: String) = LaunchedEffect(id) {
        prevEpisode = null
        prevEpisode = client.getNextEpisode(GetNextEpisodeRequest(id, SortOrder.Descending)).body().episodeInfo
    }

    var season by mutableStateOf<SeasonInfo?>(null)
        private set

    @Composable
    fun loadSeason(id: String) = LaunchedEffect(id) {
        season = null
        season = client.getSeasons(id).body()
    }

    var show by mutableStateOf<ShowInfo?>(null)
        private set

    @Composable
    fun loadShow(id: String) = LaunchedEffect(id) {
        show = null
        show = client.getShow(id).body()
    }

    var movie by mutableStateOf<MovieInfo?>(null)
        private set

    @Composable
    fun loadMovie(id: String) = LaunchedEffect(id) {
        movie = null
        movie = client.getMovie(id).body()
    }

    var library by mutableStateOf<Library?>(null)
        private set

    @Composable
    fun loadLibrary(id: String) = LaunchedEffect(id) {
        library = null
        library = client.getLibrary(id).body()
    }

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
                                        client.getEpisode(it.id).body().let { episode ->
                                            LoadedSearchResult.Episode(
                                                it,
                                                episode,
                                            )
                                        }

                                    SearchResultType.Season ->
                                        client.getSeasons(it.id).body().let { season ->
                                            LoadedSearchResult.Season(
                                                it,
                                                season,
                                            )
                                        }

                                    SearchResultType.Show -> client.getShow(it.id).body()
                                        .let { show -> LoadedSearchResult.Show(it, show) }

                                    SearchResultType.Movie -> client.getMovie(it.id).body()
                                        .let { movie -> LoadedSearchResult.Movie(it, movie) }
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
    emit(func(id).result().getOrNull())
}.flowOn(Dispatchers.IO)
