package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.browser.items
import io.rewynd.android.component.ApiImage
import io.rewynd.android.component.DefaultMediaIcon
import io.rewynd.android.util.details
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Library

@Composable
fun HomeBrowser(
    onNavigateToLibrary: (Library) -> Unit,
    onNavigateToEpisode: (EpisodeInfo) -> Unit,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val libraries = remember { viewModel.getLibraries() }.collectAsLazyPagingItems()
    val latestEpisodes = remember { viewModel.getLatestEpisodes() }.collectAsLazyPagingItems()
    val nextEpisodes = remember { viewModel.getNextEpisodes() }.collectAsLazyPagingItems()
    val newestEpisodes = remember { viewModel.getNewestEpisodes() }.collectAsLazyPagingItems()

    Column(modifier.verticalScroll(rememberScrollState())) {
        Text(text = "Libraries")
        LazyRow {
            items(libraries) {
                Card(onClick = {
                    onNavigateToLibrary(it)
                }) {
                    DefaultMediaIcon(it.name)
                    Text(text = it.name)
                }
            }
        }
        Text(text = "Continue Watching")
        LazyRow {
            items(latestEpisodes) {
                Card(onClick = {
                    onNavigateToEpisode(it.media)
                }) {
                    ApiImage(it.media.episodeImageId, loadImage = viewModel::loadImage)
                    Text(text = it.media.details)
                }
            }
        }
        Text(text = "Next Up")
        LazyRow {
            items(nextEpisodes) {
                Card(onClick = {
                    onNavigateToEpisode(it.media)
                }) {
                    ApiImage(it.media.episodeImageId, loadImage = viewModel::loadImage)
                    Text(text = it.media.details)
                }
            }
        }
        Text(text = "New Additions")
        LazyRow {
            items(newestEpisodes) {
                Card(onClick = {
                    onNavigateToEpisode(it)
                }) {
                    ApiImage(it.episodeImageId, loadImage = viewModel::loadImage)
                    Text(text = it.details)
                }
            }
        }
    }
}
