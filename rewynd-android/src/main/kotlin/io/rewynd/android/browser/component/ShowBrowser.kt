package io.rewynd.android.browser.component

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.rewynd.android.browser.BrowserState
import io.rewynd.android.component.ApiImage
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ShowBrowser(
    showInfo: ShowInfo,
    seasons: ImmutableList<SeasonInfo>,
    onBrowserStateChange: (BrowserState) -> Unit,
    modifier: Modifier = Modifier,
    loadImage: suspend (String) -> Bitmap?,
) {
    Column(modifier) {
        Text(showInfo.title, color = Color.White)
        (showInfo.plot ?: showInfo.outline)?.let { Text(it, color = Color.White) }
        showInfo.premiered?.let { Text(it, color = Color.White) }
        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp)) {
            items(seasons) {
                Card(onClick = {
                    onBrowserStateChange(BrowserState.SeasonState(it))
                }) {
                    ApiImage(it.folderImageId, loadImage = loadImage)
                    Text(text = "Season ${it.seasonNumber.toInt()}")
                }
            }
        }
    }
}
