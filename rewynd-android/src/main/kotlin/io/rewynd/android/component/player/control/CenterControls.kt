package io.rewynd.android.component.player.control

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.ui.R
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun CenterControls(
    isPlaying: Boolean,
    currentTime: Duration,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    onSeek: (desiredTime: Duration) -> Unit,
    onNext: (() -> Unit)?,
    onPrev: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(modifier = Modifier.size(40.dp), onClick = onPrev ?: { }, enabled = onPrev != null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = painterResource(id = R.drawable.exo_icon_previous),
                contentDescription = "Next",
            )
        }
        IconButton(modifier = Modifier.size(40.dp), onClick = { onSeek(currentTime - 10.seconds) }) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = painterResource(id = R.drawable.exo_icon_rewind),
                contentDescription = "Replay 10 seconds",
            )
        }

        IconButton(modifier = Modifier.size(40.dp), onClick = if (isPlaying) onPause else onPlay) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter =
                when {
                    isPlaying -> {
                        painterResource(id = R.drawable.exo_icon_pause)
                    }

                    else -> {
                        painterResource(id = R.drawable.exo_icon_play)
                    }
                },
                contentDescription = "Play/Pause",
            )
        }

        IconButton(modifier = Modifier.size(40.dp), onClick = { onSeek(currentTime + 10.seconds) }) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = painterResource(id = R.drawable.exo_icon_fastforward),
                contentDescription = "Skip 10 seconds",
            )
        }

        IconButton(modifier = Modifier.size(40.dp), onClick = onNext ?: { }, enabled = onNext != null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = painterResource(id = R.drawable.exo_icon_next),
                contentDescription = "Next",
            )
        }
    }
}
