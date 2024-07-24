package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import io.rewynd.android.browser.IBrowserNavigationActions
import io.rewynd.android.component.ApiImage
import io.rewynd.android.model.LoadedSearchResult

private const val HEIGHT_DIVISOR = 5
private const val WIDTH_DIVISOR = 4

@Composable
fun SearchResultCard(
    result: LoadedSearchResult,
    imageLoader: ImageLoader,
    actions: IBrowserNavigationActions,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        when (result) {
            is LoadedSearchResult.Episode ->
                EpisodeSearchResultCard(
                    result,
                    imageLoader,
                    actions,
                    Modifier.height(maxHeight / HEIGHT_DIVISOR),
                )

            is LoadedSearchResult.Season -> SeasonSearchResultCard(
                result,
                imageLoader,
                actions,
                Modifier.height(maxHeight / HEIGHT_DIVISOR)
            )
            is LoadedSearchResult.Show -> ShowSearchResultCard(
                result,
                imageLoader,
                actions,
                Modifier.height(maxHeight / HEIGHT_DIVISOR)
            )
        }
    }
}

@Composable
private fun EpisodeSearchResultCard(
    result: LoadedSearchResult.Episode,
    imageLoader: ImageLoader,
    actions: IBrowserNavigationActions,
    modifier: Modifier = Modifier,
) {
    Card(onClick = {
        actions.episode(result.media)
    }, modifier = modifier) {
        BoxWithConstraints {
            Row {
                ApiImage(
                    result.media.episodeImageId,
                    imageLoader,
                    Modifier.width(this@BoxWithConstraints.maxWidth / WIDTH_DIVISOR),
                )
                Column {
                    Text(result.result.title)
                    Text(result.result.description)
                }
            }
        }
    }
}

@Composable
private fun SeasonSearchResultCard(
    result: LoadedSearchResult.Season,
    imageLoader: ImageLoader,
    actions: IBrowserNavigationActions,
    modifier: Modifier = Modifier,
) {
    Card(onClick = {
        actions.season(result.media)
    }, modifier = modifier) {
        BoxWithConstraints {
            Row {
                ApiImage(
                    result.media.folderImageId,
                    imageLoader,
                    Modifier.width(this@BoxWithConstraints.maxWidth / WIDTH_DIVISOR),
                )
                Column {
                    Text(result.result.title)
                    Text(result.result.description)
                }
            }
        }
    }
}

@Composable
private fun ShowSearchResultCard(
    result: LoadedSearchResult.Show,
    imageLoader: ImageLoader,
    actions: IBrowserNavigationActions,
    modifier: Modifier = Modifier,
) {
    Card(onClick = {
        actions.show(result.media)
    }, modifier = modifier) {
        BoxWithConstraints {
            Row {
                ApiImage(
                    result.media.seriesImageId,
                    imageLoader,
                    Modifier.width(this@BoxWithConstraints.maxWidth / WIDTH_DIVISOR),
                )
                Column {
                    Text(result.result.title)
                    Text(result.result.description)
                }
            }
        }
    }
}
