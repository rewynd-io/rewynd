package io.rewynd.android.component.player.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.rewynd.android.model.PlayerMedia
import io.rewynd.model.NormalizationMethod
import kotlin.time.Duration

@Composable
fun PlayerControls(
    isVisible: Boolean,
    isPlaying: Boolean,
    title: String,
    runTime: Duration,
    currentPlayerTime: Duration,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    onSeek: (Duration) -> Unit,
    onNext: (() -> Unit)?,
    onPrev: (() -> Unit)?,
    onAudioChange: (track: String?) -> Unit,
    onVideoChange: (track: String?) -> Unit,
    onSubtitleChange: (track: String?) -> Unit,
    onNormalizationChange: (method: NormalizationMethod?) -> Unit,
    currentMedia: PlayerMedia,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.6f))) {
            TopControl(
                modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(),
                title = title,
            )

            CenterControls(
                modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                isPlaying = isPlaying,
                onNext = onNext,
                onPause = onPause,
                onPlay = onPlay,
                onPrev = onPrev,
                onSeek = onSeek,
                currentTime = currentPlayerTime,
            )

            BottomControls(
                modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .animateEnterExit(
                        enter =
                        slideInVertically(
                            initialOffsetY = { fullHeight: Int ->
                                fullHeight
                            },
                        ),
                        exit =
                        slideOutVertically(
                            targetOffsetY = { fullHeight: Int ->
                                fullHeight
                            },
                        ),
                    ),
                totalDuration = runTime,
                currentTime = currentPlayerTime,
                onSeekChange = onSeek,
                onAudioChange = onAudioChange,
                onVideoChange = onVideoChange,
                onSubtitleChange = onSubtitleChange,
                currentMedia = currentMedia,
                onNormalizationChange = onNormalizationChange,
            )
        }
    }
}
