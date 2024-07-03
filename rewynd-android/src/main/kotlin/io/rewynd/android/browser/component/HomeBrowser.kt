package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
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
@Suppress("MagicNumber") // TODO fix magic numbers
fun HomeBrowser(
    onNavigateToLibrary: (Library) -> Unit,
    onNavigateToEpisode: (EpisodeInfo) -> Unit,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    val libraries = remember { viewModel.listLibraries() }.collectAsLazyPagingItems()
    val latestEpisodes = remember { viewModel.listRecentlyWatchedEpisodes() }.collectAsLazyPagingItems()
    val nextEpisodes = remember { viewModel.listNextEpisodes() }.collectAsLazyPagingItems()
    val newestEpisodes = remember { viewModel.listRecentlyAddedEpisodes() }.collectAsLazyPagingItems()
    BoxWithConstraints Outer@{
        Column(modifier.verticalScroll(rememberScrollState())) {
            BoxWithConstraints(Modifier.height(this@Outer.maxHeight / 4)) Inner@{
                Column {
                    Text(modifier = Modifier.height(this@Inner.maxHeight / 4), text = "Libraries")
                    LazyRow(modifier = Modifier.height((this@Inner.maxHeight / 4) * 3)) {
                        items(libraries) {
                            BoxWithConstraints Item@{
                                Card(onClick = {
                                    onNavigateToLibrary(it)
                                }) {
                                    DefaultMediaIcon(
                                        it.name,
                                        modifier = Modifier.height((this@Item.maxHeight / 4) * 3),
                                    )
                                    Text(modifier = Modifier.height((this@Item.maxHeight / 4)), text = it.name)
                                }
                            }
                        }
                    }
                }
            }
            BoxWithConstraints(Modifier.height(this@Outer.maxHeight / 4)) Inner@{
                Column {
                    Text(modifier = Modifier.height(this@Inner.maxHeight / 4), text = "Continue Watching")
                    LazyRow(modifier = Modifier.height((this@Inner.maxHeight / 4) * 3)) {
                        items(latestEpisodes) {
                            BoxWithConstraints Item@{
                                Card(onClick = {
                                    onNavigateToEpisode(it.media)
                                }) {
                                    ApiImage(
                                        it.media.episodeImageId,
                                        modifier = Modifier.height((this@Item.maxHeight / 4) * 3),
                                        loadImage = viewModel::loadImage,
                                    )
                                    Text(modifier = Modifier.height((this@Item.maxHeight / 4)), text = it.media.details)
                                }
                            }
                        }
                    }
                }
            }
            BoxWithConstraints(Modifier.height(this@Outer.maxHeight / 4)) Inner@{
                Column {
                    Text(modifier = Modifier.height(this@Inner.maxHeight / 4), text = "Next Up")
                    LazyRow(modifier = Modifier.height((this@Inner.maxHeight / 4) * 3)) {
                        items(nextEpisodes) {
                            BoxWithConstraints Item@{
                                Card(onClick = {
                                    onNavigateToEpisode(it.media)
                                }) {
                                    ApiImage(
                                        it.media.episodeImageId,
                                        modifier = Modifier.height((this@Item.maxHeight / 4) * 3),
                                        loadImage = viewModel::loadImage,
                                    )
                                    Text(modifier = Modifier.height((this@Item.maxHeight / 4)), text = it.media.details)
                                }
                            }
                        }
                    }
                }
            }
            BoxWithConstraints(Modifier.height(this@Outer.maxHeight / 4)) Inner@{
                Column {
                    Text(modifier = Modifier.height(this@Inner.maxHeight / 4), text = "New Additions")
                    LazyRow(modifier = Modifier.height((this@Inner.maxHeight / 4) * 3)) {
                        items(newestEpisodes) {
                            BoxWithConstraints Item@{
                                Card(onClick = {
                                    onNavigateToEpisode(it)
                                }) {
                                    ApiImage(
                                        it.episodeImageId,
                                        modifier = Modifier.height((this@Item.maxHeight / 4) * 3),
                                        loadImage = viewModel::loadImage,
                                    )
                                    Text(modifier = Modifier.height((this@Item.maxHeight / 4)), text = it.details)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
