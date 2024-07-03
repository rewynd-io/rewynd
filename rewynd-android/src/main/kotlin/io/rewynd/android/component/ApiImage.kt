package io.rewynd.android.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Suppress("ModifierReused")
@Composable
fun ApiImage(
    imageId: String?,
    modifier: Modifier = Modifier,
    loadImage: (String) -> Flow<Bitmap?> = {flowOf( null )},
) {
    val bitmap by imageId?.let { loadImage.invoke(it).collectAsStateWithLifecycle(null) } ?: remember { mutableStateOf(null) }

    // KtLint doesn't understand that the modifier is being used at the root level here.
    bitmap?.let { Image(it.asImageBitmap(), imageId, modifier) } ?: DefaultMediaIcon(imageId, modifier)
}
