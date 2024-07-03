package io.rewynd.android.browser.component

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.browser.items
import io.rewynd.android.component.ApiImage
import io.rewynd.model.ShowInfo

@Composable
fun LibraryBrowser(
    libraryName: String,
    onNavigateToShow: (ShowInfo) -> Unit,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val shows = remember { viewModel.listShows(libraryName) }.collectAsLazyPagingItems()

    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier) {
        items(shows) {
            Card(onClick = {
                onNavigateToShow(it)
            }) {
                ApiImage(it.seriesImageId, Modifier, viewModel::loadImage)
                Text(text = it.title)
            }
        }
    }
}
