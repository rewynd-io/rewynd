package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import coil3.ImageLoader
import io.rewynd.android.browser.items
import io.rewynd.android.component.ApiImage

private const val DIVISOR = 4
private const val REMAINDER = DIVISOR - 1

@Composable
fun <Item : Any> HomeRow(
    title: String,
    libraries: LazyPagingItems<Item>,
    nameAccessor: Item.() -> String,
    loadImage: ImageLoader,
    modifier: Modifier = Modifier,
    imageIdAccessor: Item.() -> String? = { null },
    onNavigateToLibrary: (Item) -> Unit = {}
) {
    BoxWithConstraints(modifier) Inner@{
        Column {
            Text(modifier = Modifier.height(this@Inner.maxHeight / DIVISOR), text = title)
            LazyRow(modifier = Modifier.height((this@Inner.maxHeight / DIVISOR) * REMAINDER)) {
                items(libraries) {
                    BoxWithConstraints Item@{
                        Card(onClick = {
                            onNavigateToLibrary(it)
                        }) {
                            ApiImage(
                                it.imageIdAccessor(),
                                loadImage = loadImage,
                                modifier = Modifier.height((this@Item.maxHeight / DIVISOR) * REMAINDER),
                            )
                            Text(modifier = Modifier.height((this@Item.maxHeight / DIVISOR)), text = it.nameAccessor())
                        }
                    }
                }
            }
        }
    }
}
