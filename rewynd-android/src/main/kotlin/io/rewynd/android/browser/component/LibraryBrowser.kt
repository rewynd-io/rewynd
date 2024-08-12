package io.rewynd.android.browser.component

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.ImageLoader
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.browser.IBrowserNavigationActions
import io.rewynd.android.browser.items
import io.rewynd.android.component.ApiImage
import io.rewynd.model.Library
import io.rewynd.model.LibraryType
import io.rewynd.model.MovieInfo
import io.rewynd.model.ShowInfo

@Composable
fun LibraryBrowser(
    library: Library,
    actions: IBrowserNavigationActions,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val movies = remember { viewModel.listMovies(library.name) }.collectAsLazyPagingItems()
    val shows = remember { viewModel.listShows(library.name) }.collectAsLazyPagingItems()

    when (library.type) {
        LibraryType.Movie -> MovieLibraryBrowser(actions, movies, viewModel.imageLoader, modifier)
        LibraryType.Show -> ShowLibraryBrowser(actions, shows, viewModel.imageLoader, modifier)
        LibraryType.Image -> TODO()
    }
}

@Composable
private fun ShowLibraryBrowser(
    actions: IBrowserNavigationActions,
    shows: LazyPagingItems<ShowInfo>,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier) {
        items(shows) {
            Card(onClick = {
                actions.show(it)
            }) {
                ApiImage(it.seriesImageId, imageLoader, Modifier)
                Text(text = it.title)
            }
        }
    }
}

@Composable
private fun MovieLibraryBrowser(
    actions: IBrowserNavigationActions,
    movies: LazyPagingItems<MovieInfo>,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier) {
        items(movies) {
            Card(onClick = {
                actions.movie(it)
            }) {
                ApiImage(it.posterImageId, imageLoader, Modifier)
                Text(text = it.title)
            }
        }
    }
}
