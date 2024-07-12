package io.rewynd.android.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.rewynd.android.R
import io.rewynd.android.model.ImageId

@Suppress("ModifierReused")
@Composable
fun ApiImage(
    imageId: String?,
    loadImage: ImageLoader,
    modifier: Modifier = Modifier,
) {
    imageId?.let {
        AsyncImage(
            model =
            ImageRequest
                .Builder(LocalContext.current)
                .data(ImageId(it))
                .build(),
            contentDescription = null, // TODO set description
            imageLoader = loadImage,
            placeholder = painterResource(R.drawable.rewynd_default_media_icon),
            modifier = modifier,
        )
    } ?: DefaultMediaIcon(
        null, // TODO set description
        modifier,
    )
}
