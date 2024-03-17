package io.rewynd.android.browser.component

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.rewynd.android.browser.BrowserState
import io.rewynd.android.component.ApiImage
import io.rewynd.android.component.DefaultMediaIcon
import io.rewynd.android.util.details
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Library
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBrowser(
    onBrowserStateChange: (BrowserState) -> Unit,
    libraries: ImmutableList<Library>,
    latestEpisodes: ImmutableList<EpisodeInfo>,
    nextEpisodes: ImmutableList<EpisodeInfo>,
    newestEpisodes: ImmutableList<EpisodeInfo>,
    modifier: Modifier = Modifier,
    loadImage: suspend (String) -> Bitmap?,
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        Text(text = "Libraries")
        Row {
            libraries.forEach {
                Card(onClick = {
                    onBrowserStateChange(BrowserState.LibraryState(it))
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
                    onBrowserStateChange(BrowserState.EpisodeState(it))
                }) {
                    ApiImage(it.episodeImageId, loadImage = loadImage)
                    Text(text = it.details)
                }
            }
        }
        Text(text = "Next Up")
        LazyRow {
            items(nextEpisodes) {
                Card(onClick = {
                    onBrowserStateChange(BrowserState.EpisodeState(it))
                }) {
                    ApiImage(it.episodeImageId, loadImage = loadImage)
                    Text(text = it.details)
                }
            }
        }
        Text(text = "New Additions")
        LazyRow {
            items(newestEpisodes) {
                Card(onClick = {
                    onBrowserStateChange(BrowserState.EpisodeState(it))
                }) {
                    ApiImage(it.episodeImageId, loadImage = loadImage)
                    Text(text = it.details)
                }
            }
        }
    }
}
