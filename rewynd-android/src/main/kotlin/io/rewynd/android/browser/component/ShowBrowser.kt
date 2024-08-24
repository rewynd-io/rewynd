package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import io.rewynd.android.browser.BrowserNavigationActions
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.browser.items
import io.rewynd.android.component.ApiImage

@Composable
fun ShowBrowser(
    showId: String,
    viewModel: BrowserViewModel,
    actions: BrowserNavigationActions,
    modifier: Modifier = Modifier
) = with(viewModel) {
    loadShow(showId)

    val seasons = remember { listSeasons(showId) }.collectAsLazyPagingItems()

    show?.let { showInfo ->

        Column(modifier) {
            Row {
                Button({
                    actions.library(showInfo.libraryId)
                }) {
                    Text(showInfo.libraryId, color = Color.White)
                }
                Text(showInfo.title, color = Color.White)
            }
            (showInfo.plot ?: showInfo.outline)?.let { Text(it, color = Color.White) }
            showInfo.premiered?.let { Text(it.toString(), color = Color.White) }
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp)) {
                items(seasons) {
                    Card(onClick = {
                        actions.season(it.id)
                    }) {
                        ApiImage(it.folderImageId, loadImage = viewModel.imageLoader)
                        Text(text = "Season ${it.seasonNumber}")
                    }
                }
            }
        }
    } ?: CircularProgressIndicator()
}
