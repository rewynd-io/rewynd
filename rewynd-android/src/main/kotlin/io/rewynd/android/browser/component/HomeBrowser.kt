package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.util.details
import io.rewynd.model.Library

@Composable
@Suppress("MagicNumber") // TODO fix magic numbers
fun HomeBrowser(
    onNavigateToLibrary: (String) -> Unit,
    onNavigateToEpisode: (String) -> Unit,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    val libraries = remember { viewModel.listLibraries() }.collectAsLazyPagingItems()
    val latestEpisodes = remember { viewModel.listRecentlyWatchedEpisodes() }.collectAsLazyPagingItems()
    val nextEpisodes = remember { viewModel.listNextEpisodes() }.collectAsLazyPagingItems()
    val newestEpisodes = remember { viewModel.listRecentlyAddedEpisodes() }.collectAsLazyPagingItems()
    BoxWithConstraints {
        Column(modifier.verticalScroll(rememberScrollState())) {
            HomeRow(
                "Libraries",
                libraries,
                Library::name,
                Library::name,
                viewModel.imageLoader,
                Modifier.height(this@BoxWithConstraints.maxHeight / 4),
                { null },
                onNavigateToLibrary,
            )
            HomeRow(
                "Continue Watching",
                latestEpisodes,
                { media.details },
                { media.id },
                viewModel.imageLoader,
                Modifier.height(this@BoxWithConstraints.maxHeight / 4),
                { media.episodeImageId },
                onNavigateToEpisode
            )
            HomeRow(
                "Next Up",
                nextEpisodes,
                { this.details },
                { this.id },
                viewModel.imageLoader,
                Modifier.height(this@BoxWithConstraints.maxHeight / 4),
                { this.episodeImageId },
                onNavigateToEpisode
            )
            HomeRow(
                "New Additions",
                newestEpisodes,
                { details },
                { id },
                viewModel.imageLoader,
                Modifier.height(this@BoxWithConstraints.maxHeight / 4),
                { episodeImageId },
                onNavigateToEpisode
            )
        }
    }
}
