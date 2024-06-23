package io.rewynd.android.browser

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems

fun <T : Any> LazyListScope.items(
    lazyPagingItems: LazyPagingItems<T>,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    this.items(lazyPagingItems.itemCount) { index ->
        lazyPagingItems[index]?.let { item ->
            itemContent(item)
        }
    }
}
fun <T : Any> LazyGridScope.items(
    lazyPagingItems: LazyPagingItems<T>,
    itemContent: @Composable LazyGridItemScope.(T) -> Unit
) {
    this.items(lazyPagingItems.itemCount) { index ->
        lazyPagingItems[index]?.let { item ->
            itemContent(item)
        }
    }
}
