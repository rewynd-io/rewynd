package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import io.rewynd.android.browser.BrowserNavigationActions
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.browser.items
import io.rewynd.android.component.ApiImage
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.SeasonInfo

@Composable
fun SeasonBrowser(
    seasonInfo: SeasonInfo,
    viewModel: BrowserViewModel,
    onNavigateToEpisode: (EpisodeInfo) -> Unit,
    actions: BrowserNavigationActions,
    modifier: Modifier = Modifier
) {
    val episodes = remember { viewModel.listEpisodes(seasonInfo.id) }.collectAsLazyPagingItems()
    val showInfo by viewModel.getShow(seasonInfo.showId).collectAsStateWithLifecycle(null)
    val library by viewModel.getLibrary(seasonInfo.libraryId).collectAsStateWithLifecycle(null)

    Column(modifier) {
        Row {
            Button({
                library?.let {
                    actions.library(it)
                }
            }) {
                Text(seasonInfo.libraryId, color = Color.White)
            }
            Button({
                showInfo?.let {
                    actions.show(it)
                }
            }) {
                seasonInfo.showName?.let { Text(it, color = Color.White) }
            }
            Text("Season ${seasonInfo.seasonNumber}", color = Color.White)
        }

        Text(seasonInfo.showName, color = Color.White)
        Text("Season ${seasonInfo.seasonNumber.toInt()}", color = Color.White)
        seasonInfo.year?.toInt()?.toString()?.let { Text(it, color = Color.White) }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
        ) {
            items(episodes) {
                Card(onClick = {
                    onNavigateToEpisode(it)
                }) {
                    ApiImage(it.episodeImageId, loadImage = viewModel::loadImage)
                    Text(text = it.title)
                }
            }
        }
    }
}
