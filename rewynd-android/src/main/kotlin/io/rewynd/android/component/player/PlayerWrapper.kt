package io.rewynd.android.component.player

import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import io.rewynd.android.component.player.control.PlayerControls
import io.rewynd.android.player.PlayerServiceInterface
import io.rewynd.android.player.PlayerState
import io.rewynd.android.player.PlayerViewModel
import io.rewynd.android.player.StreamHeartbeat.Companion.copy
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Suppress("MultipleEmitters")
@Composable
fun PlayerWrapper(
    viewModel: PlayerViewModel,
    state: PlayerState,
    serviceInterface: PlayerServiceInterface,
    setBoundingRect: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    updateMedia: () -> Unit,
) {
    fun View.useRect() {
        val rect = Rect()
        this.getGlobalVisibleRect(
            rect,
        )
        setBoundingRect(rect)
    }

    val areControlsVisible by viewModel.areControlsVisible.collectAsState()

    state.media?.let { media ->
        Log.d("PlayerActivity", media.toString())
        LaunchedEffect(key1 = media, key2 = updateMedia) {
            updateMedia()
        }
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidView(
                modifier =
                Modifier.clickable {
                    viewModel.setControlsVisible(areControlsVisible.not())
                }.background(Color.Black).fillMaxHeight().fillMaxWidth(),
                factory = { context ->
                    serviceInterface.getPlayerView(context).apply {
                        useController = false
                        this.addOnLayoutChangeListener { view: View,
                                                         _: Int,
                                                         _: Int,
                                                         _: Int,
                                                         _: Int,
                                                         _: Int,
                                                         _: Int,
                                                         _: Int,
                                                         _: Int ->
                            view.useRect()
                        }
                        this.useRect()
                    }
                },
            )
            // TODO reset controls visibility on any button press
            PlayerControls(
                modifier = Modifier.fillMaxSize(),
                isVisible = areControlsVisible,
                isPlaying = state.isPlaying,
                title = media.details,
                onPrev =
                if (state.prev == null) {
                    null
                } else {
                    { serviceInterface.playPrev() }
                },
                onNext =
                if (state.next == null) {
                    null
                } else {
                    { serviceInterface.playNext() }
                },
                onPlay = { serviceInterface.play() },
                onPause = { serviceInterface.pause() },
                onSeek = { serviceInterface.seek(it) },
                currentPlayerTime = state.offsetTime,
                runTime = media.runTime,
                onAudioChange = {
                    MainScope().launch {
                        serviceInterface.loadMedia(
                            media.copy(
                                audioTrackName = it,
                                startOffset = state.offsetTime,
                            ),
                        )
                    }
                },
                onVideoChange = {
                    MainScope().launch {
                        serviceInterface.loadMedia(
                            media.copy(
                                videoTrackName = it,
                                startOffset = state.offsetTime,
                            ),
                        )
                    }
                },
                onSubtitleChange = {
                    MainScope().launch {
                        serviceInterface.loadMedia(
                            media.copy(
                                subtitleTrackName = it,
                                startOffset = state.offsetTime,
                            ),
                        )
                    }
                },
                currentMedia = media,
                onNormalizationChange = {
                    MainScope().launch {
                        serviceInterface.loadMedia(
                            media.copy(
                                normalizationMethod = it,
                                startOffset = state.offsetTime
                            ),
                        )
                    }
                },
            )
        }
    }

    if (state.isLoading) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(modifier = Modifier.background(Color.Transparent))
        }
    }
}
