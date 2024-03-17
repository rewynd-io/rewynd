package io.rewynd.android.browser.component

import android.graphics.Bitmap
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rewynd.android.browser.BrowserState
import io.rewynd.android.component.ApiImage
import io.rewynd.model.ShowInfo
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryBrowser(
    shows: ImmutableList<ShowInfo>,
    onBrowserStateChange: (BrowserState) -> Unit,
    modifier: Modifier = Modifier,
    loadImage: suspend (String) -> Bitmap?,
) {
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), modifier) {
        items(shows!!) {
            Card(onClick = {
                onBrowserStateChange(BrowserState.ShowState(it))
            }) {
                ApiImage(it.seriesImageId, Modifier, loadImage)
                Text(text = it.title)
            }
        }
    }
}
