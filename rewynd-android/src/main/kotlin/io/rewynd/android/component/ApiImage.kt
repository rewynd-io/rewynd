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

@Suppress("ktlint:compose:modifier-reused-check")
@Composable
fun ApiImage(
    imageId: String?,
    modifier: Modifier = Modifier,
    loadImage: suspend (String) -> Bitmap? = { null },
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(imageId, loadImage) {
        imageId?.let { loadImage(it) }?.let { bitmap = it }
    }

    // KtLint doesn't understand that the modifier is being used at the root level here.
    bitmap?.let { Image(it.asImageBitmap(), imageId, modifier) } ?: DefaultMediaIcon(imageId, modifier)
}
