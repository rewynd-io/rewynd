package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.browser.items
import io.rewynd.android.component.ApiImage
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo

@Composable
fun ShowBrowser(
    showInfo: ShowInfo,
    viewModel: BrowserViewModel,
    onNavigateToSeason: (SeasonInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val seasons = remember { viewModel.getSeasons(showInfo.id) }.collectAsLazyPagingItems()

    Column(modifier) {
        Text(showInfo.title, color = Color.White)
        (showInfo.plot ?: showInfo.outline)?.let { Text(it, color = Color.White) }
        showInfo.premiered?.let { Text(it, color = Color.White) }
        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp)) {
            items(seasons) {
                Card(onClick = {
                    onNavigateToSeason(it)
                }) {
                    ApiImage(it.folderImageId, loadImage = viewModel::loadImage)
                    Text(text = "Season ${it.seasonNumber.toInt()}")
                }
            }
        }
    }
}
