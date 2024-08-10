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
import io.rewynd.android.browser.BrowserNavigationActions
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.browser.IBrowserNavigationActions
import io.rewynd.android.browser.items
import io.rewynd.android.component.ApiImage
import io.rewynd.model.Library
import io.rewynd.model.LibraryType
import io.rewynd.model.ShowInfo

@Composable
fun LibraryBrowser(
    library: Library,
    actions: IBrowserNavigationActions,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    when(library.type) {
        LibraryType.Movie -> MovieLibraryBrowser(library, actions, viewModel, modifier)
        LibraryType.Show -> ShowLibraryBrowser(library, actions, viewModel, modifier)
        LibraryType.Image -> TODO()
    }
}

@Composable
private fun ShowLibraryBrowser(
    library: Library,
    actions: IBrowserNavigationActions,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val shows = remember { viewModel.listShows(library.name) }.collectAsLazyPagingItems()

    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier) {
        items(shows) {
            Card(onClick = {
                actions.show(it)
            }) {
                ApiImage(it.seriesImageId, viewModel.imageLoader, Modifier)
                Text(text = it.title)
            }
        }
    }
}

@Composable
private fun MovieLibraryBrowser(
    library: Library,
    actions: IBrowserNavigationActions,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val shows = remember { viewModel.listMovies(library.name) }.collectAsLazyPagingItems()

    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier) {
        items(shows) {
            Card(onClick = {
                actions.movie(it)
            }) {
                ApiImage(it.posterImageId, viewModel.imageLoader, Modifier)
                Text(text = it.title)
            }
        }
    }
}