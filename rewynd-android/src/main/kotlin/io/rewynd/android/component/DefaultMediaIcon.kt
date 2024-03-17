package io.rewynd.android.component

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import io.rewynd.android.App
import io.rewynd.android.R
import kotlin.math.min

private val defaultBitmap by lazy {
    val drawable = requireNotNull(ContextCompat.getDrawable(App.context, R.drawable.rewynd_default_media_icon))
    val width = min(500, drawable.intrinsicWidth)
    val height = min(500, drawable.intrinsicHeight)
    val bitmap =
        Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888,
        )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    bitmap.asImageBitmap()
}

@Composable
fun DefaultMediaIcon(
    description: String?,
    modifier: Modifier = Modifier,
) {
    Image(defaultBitmap, description, modifier)
}
