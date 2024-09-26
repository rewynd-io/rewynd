package io.rewynd.android.component.player.control

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlignVerticalCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Modifier
import io.rewynd.android.model.PlayerMedia
import io.rewynd.model.NormalizationMethod
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun BottomControls(
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
