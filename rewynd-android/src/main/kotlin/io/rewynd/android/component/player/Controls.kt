package io.rewynd.android.component.player

import android.text.format.DateUtils
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlignVerticalCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.rewynd.android.model.PlayerMedia
import io.rewynd.model.NormalizationMethod
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerControls(
    isVisible: Boolean,
    isPlaying: Boolean,
    title: String,
    runTime: Duration,
    currentPlayerTime: Duration,
    bufferedPosition: Duration,
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
    Log.i("PlayerControls", "$runTime, $currentPlayerTime, $bufferedPosition, $title")

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

@Composable
private fun TopControl(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.padding(16.dp),
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color(1.0f, 1.0f, 1.0f),
    )
}

@Composable
private fun CenterControls(
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
                painter = painterResource(id = androidx.media3.ui.R.drawable.exo_icon_previous),
                contentDescription = "Next",
            )
        }
        IconButton(modifier = Modifier.size(40.dp), onClick = { onSeek(currentTime - 10.seconds) }) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = painterResource(id = androidx.media3.ui.R.drawable.exo_icon_rewind),
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
                        painterResource(id = androidx.media3.ui.R.drawable.exo_icon_pause)
                    }

                    else -> {
                        painterResource(id = androidx.media3.ui.R.drawable.exo_icon_play)
                    }
                },
                contentDescription = "Play/Pause",
            )
        }

        IconButton(modifier = Modifier.size(40.dp), onClick = { onSeek(currentTime + 10.seconds) }) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = painterResource(id = androidx.media3.ui.R.drawable.exo_icon_fastforward),
                contentDescription = "Skip 10 seconds",
            )
        }

        IconButton(modifier = Modifier.size(40.dp), onClick = onNext ?: { }, enabled = onNext != null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                painter = painterResource(id = androidx.media3.ui.R.drawable.exo_icon_next),
                contentDescription = "Next",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomControls(
    totalDuration: Duration,
    currentTime: Duration,
    onSeekChange: (desiredTime: Duration) -> Unit,
    onAudioChange: (track: String?) -> Unit,
    onVideoChange: (track: String?) -> Unit,
    onSubtitleChange: (track: String?) -> Unit,
    onNormalizationChange: (method: NormalizationMethod?) -> Unit,
    currentMedia: PlayerMedia,
    modifier: Modifier = Modifier,
) {
    var audioExpanded by remember { mutableStateOf(false) }
    var videoExpanded by remember { mutableStateOf(false) }
    var subtitleExpanded by remember { mutableStateOf(false) }
    var normalizationExpanded by remember { mutableStateOf(false) }

    var sliderValue by remember { mutableStateOf(currentTime.inWholeMilliseconds.toFloat()) }
    var sliderIsUp by remember { mutableStateOf(false) }
    LaunchedEffect(sliderIsUp, currentTime) {
        if (!sliderIsUp) {
            sliderValue = currentTime.inWholeMilliseconds.toFloat()
        }
    }

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // TODO show bufferedPosition on slider
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = sliderValue,
                onValueChange = {
                    sliderIsUp = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    sliderIsUp = false
                    onSeekChange(sliderValue.roundToLong().milliseconds)
                },
                valueRange = 0f..totalDuration.inWholeMilliseconds.toFloat(),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { audioExpanded = !audioExpanded }) {
                Icon(Icons.Default.MusicNote, "Audio Track", tint = MaterialTheme.colorScheme.primary)
            }
            DropdownMenu(expanded = audioExpanded, onDismissRequest = { audioExpanded = false }) {
                DropdownMenuItem(
                    {
                        Text(text = "None")
                    },
                    onClick = {
                        onAudioChange(null)
                        audioExpanded = false
                    },
                )
                currentMedia.audioTracks.keys.forEach { audioTrack ->
                    DropdownMenuItem(
                        trailingIcon = {
                            if (audioTrack == currentMedia.audioTrackName) {
                                Icon(
                                    Icons.Default.Check,
                                    "Selected audio track",
                                )
                            }
                        },
                        text = {
                            Text(text = audioTrack)
                        },
                        onClick = {
                            onAudioChange(audioTrack)
                            audioExpanded = false
                        },
                    )
                }
            }
            IconButton(onClick = { videoExpanded = !videoExpanded }) {
                Icon(Icons.Default.Tv, "Video Track", tint = MaterialTheme.colorScheme.primary)
            }
            DropdownMenu(expanded = videoExpanded, onDismissRequest = { videoExpanded = false }) {
                DropdownMenuItem(
                    {
                        Text(text = "None")
                    },
                    onClick = {
                        onVideoChange(null)
                        videoExpanded = false
                    },
                )
                currentMedia.videoTracks.keys.forEach { videoTrack ->
                    DropdownMenuItem(
                        trailingIcon = {
                            if (videoTrack == currentMedia.videoTrackName) {
                                Icon(
                                    Icons.Default.Check,
                                    "Selected video track",
                                )
                            }
                        },
                        text = {
                            Text(text = videoTrack)
                        },
                        onClick = {
                            onVideoChange(videoTrack)
                            videoExpanded = false
                        },
                    )
                }
            }
            IconButton(onClick = { subtitleExpanded = !subtitleExpanded }) {
                Icon(Icons.Default.Subtitles, "Subtitle Track", tint = MaterialTheme.colorScheme.primary)
            }
            DropdownMenu(expanded = subtitleExpanded, onDismissRequest = { subtitleExpanded = false }) {
                DropdownMenuItem(
                    {
                        Text(text = "None")
                    },
                    onClick = {
                        onSubtitleChange(null)
                        subtitleExpanded = false
                    },
                )
                currentMedia.subtitleTracks.keys.forEach { subtitleTrack ->
                    DropdownMenuItem(
                        trailingIcon = {
                            if (subtitleTrack == currentMedia.subtitleTrackName) {
                                Icon(
                                    Icons.Default.Check,
                                    "Selected subtitle track",
                                )
                            }
                        },
                        text = {
                            Text(text = subtitleTrack)
                        },
                        onClick = {
                            onSubtitleChange(subtitleTrack)
                            subtitleExpanded = false
                        },
                    )
                }
            }
            IconButton(onClick = { normalizationExpanded = !normalizationExpanded }) {
                Icon(Icons.Default.AlignVerticalCenter, "Audio Normalization", tint = MaterialTheme.colorScheme.primary)
            }
            DropdownMenu(expanded = normalizationExpanded, onDismissRequest = { normalizationExpanded = false }) {
                DropdownMenuItem(
                    {
                        Text(text = "None")
                    },
                    onClick = {
                        onNormalizationChange(null)
                        normalizationExpanded = false
                    },
                )
                NormalizationMethod.entries.forEach { normalizationMethod ->
                    DropdownMenuItem(
                        trailingIcon = {
                            if (normalizationMethod == currentMedia.normalizationMethod) {
                                Icon(
                                    Icons.Default.Check,
                                    "Selected normalization method",
                                )
                            }
                        },
                        text = {
                            Text(text = normalizationMethod.value)
                        },
                        onClick = {
                            onNormalizationChange(normalizationMethod)
                            normalizationExpanded = false
                        },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${DateUtils.formatElapsedTime(currentTime.inWholeSeconds)} / ${
                    DateUtils.formatElapsedTime(
                        totalDuration.inWholeSeconds,
                    )
                }",
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
