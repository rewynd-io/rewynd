package io.rewynd.android.browser.component

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.rewynd.android.browser.BrowserState
import io.rewynd.android.component.ApiImage
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.SeasonInfo
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonBrowser(
    seasonInfo: SeasonInfo,
    episodes: ImmutableList<EpisodeInfo>,
    onBrowserStateChange: (BrowserState) -> Unit,
    modifier: Modifier = Modifier,
    loadImage: suspend (String) -> Bitmap?,
) {
    Column(modifier) {
        Text(seasonInfo.showName, color = Color.White)
        Text("Season ${seasonInfo.seasonNumber.toInt()}", color = Color.White)
        seasonInfo.year?.toInt()?.toString()?.let { Text(it, color = Color.White) }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
        ) {
            items(episodes) {
                Card(onClick = {
                    onBrowserStateChange(BrowserState.EpisodeState(it))
                }) {
                    ApiImage(it.episodeImageId, loadImage = loadImage)
                    Text(text = it.title)
                }
            }
        }
    }
}
