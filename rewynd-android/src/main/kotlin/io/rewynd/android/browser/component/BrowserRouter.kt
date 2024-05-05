package io.rewynd.android.browser.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import io.rewynd.android.browser.BrowserState
import io.rewynd.android.browser.BrowserViewModel
import kotlinx.collections.immutable.toImmutableList

@Composable
fun BrowserRouter(
    mainViewModel: BrowserViewModel,
    onFinish: () -> Unit,
) {
    val mutableState by mainViewModel.browserState.collectAsState()
    val libraries by mainViewModel.libraries.observeAsState()
    val latestEpisodes by mainViewModel.latestEpisodes.observeAsState()
    val nextEpisodes by mainViewModel.nextEpisodes.observeAsState()
    val newestEpisodes by mainViewModel.newestEpisodes.observeAsState()
    val nextEpisode by mainViewModel.nextEpisode.observeAsState()
    val previousEpisode by mainViewModel.previousEpisode.observeAsState()
    val progress by mainViewModel.userProgress.observeAsState()
    val shows by mainViewModel.shows.observeAsState()
    val seasons by mainViewModel.seasons.observeAsState()
    val episodes by mainViewModel.episodes.observeAsState()
    val backStack by mainViewModel.browserState.collectAsState()
    when (val state = mutableState.last()) { // TODO deal with nullable
        is BrowserState.HomeState ->
            HomeBrowser(
                mainViewModel::putBrowserState,
                (libraries ?: emptyList()).toImmutableList(),
                (latestEpisodes ?: emptyList()).toImmutableList(),
                (nextEpisodes ?: emptyList()).toImmutableList(),
                (newestEpisodes ?: emptyList()).toImmutableList(),
                loadImage = mainViewModel::loadImage,
            )

        is BrowserState.LibraryState ->
            LibraryBrowser(
                (shows ?: emptyList()).toImmutableList(),
                mainViewModel::putBrowserState,
                loadImage = mainViewModel::loadImage,
            )

        is BrowserState.ShowState ->
            ShowBrowser(
                state.showInfo,
                (seasons ?: emptyList()).toImmutableList(),
                mainViewModel::putBrowserState,
                loadImage = mainViewModel::loadImage,
            )

        is BrowserState.SeasonState ->
            SeasonBrowser(
                state.seasonInfo,
                (episodes ?: emptyList()).toImmutableList(),
                mainViewModel::putBrowserState,
                loadImage = mainViewModel::loadImage,
            )

        is BrowserState.EpisodeState ->
            EpisodeBrowser(
                nextEpisode,
                previousEpisode,
                progress,
                state.episodeInfo,
                backStack.toImmutableList(),
                mainViewModel.serverUrl,
                loadImage = mainViewModel::loadImage,
                onFinish = onFinish,
            )
    }
}
