package io.rewynd.android.browser.component

import android.graphics.Bitmap
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import io.rewynd.android.browser.items
import io.rewynd.android.component.ApiImage
import io.rewynd.android.component.DefaultMediaIcon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun <Item : Any> HomeRow(
    title: String,
    libraries: LazyPagingItems<Item>,
    nameAccessor: Item.() -> String,
    modifier: Modifier = Modifier,
    imageIdAccessor: Item.() -> String? = { null },
    loadImage: (String) -> Flow<Bitmap?> = { flowOf(null) },
    onNavigateToLibrary: (Item) -> Unit = {}
) {
    BoxWithConstraints(modifier) Inner@{
        Column {
            Text(modifier = Modifier.height(this@Inner.maxHeight / 4), text = title)
            LazyRow(modifier = Modifier.height((this@Inner.maxHeight / 4) * 3)) {
                items(libraries) {
                    BoxWithConstraints Item@{
                        Card(onClick = {
                            onNavigateToLibrary(it)
                        }) {
                            ApiImage(
                                it.imageIdAccessor(),
                                modifier = Modifier.height((this@Item.maxHeight / 4) * 3),
                                loadImage = loadImage,
                            )
                            DefaultMediaIcon(
                                it.nameAccessor(),
                                modifier = Modifier.height((this@Item.maxHeight / 4) * 3),
                            )
                            Text(modifier = Modifier.height((this@Item.maxHeight / 4)), text = it.nameAccessor())
                        }
                    }
                }
            }
        }
    }
}