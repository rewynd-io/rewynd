package io.rewynd.android.player

import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import io.rewynd.android.browser.BrowserActivity
import io.rewynd.android.component.player.PlayerControls
import io.rewynd.android.player.StreamHeartbeat.Companion.copy
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PlayerActivity : AppCompatActivity() {
    private var lastProps: PlayerActivityProps? = null
    private lateinit var viewModel: PlayerViewModel

    private val playerService: PlayerServiceInterface?
        get() = PlayerService.instance.value

    private fun updatePictureInPictureParams() {
        val next =
            if (playerService?.next?.value != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                playerService?.nextPendingIntent?.let { nextPendingIntent ->
                    RemoteAction(
                        Icon.createWithResource(
                            this@PlayerActivity,
                            androidx.media3.ui.R.drawable.exo_icon_next,
                        ),
                        "Next",
                        "Next",
                        nextPendingIntent,
                    )
                }
            } else {
                null
            }

        val prev =
            if (playerService?.prev?.value != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                playerService?.prevPendingIntent?.let { prevPendingIntent ->
                    RemoteAction(
                        Icon.createWithResource(
                            this@PlayerActivity,
                            androidx.media3.ui.R.drawable.exo_icon_previous,
                        ),
                        "Prev",
                        "Prev",
                        prevPendingIntent,
                    )
                }
            } else {
                null
            }

        val pausePendingIntent = playerService?.pausePendingIntent
        val playPendingIntent = playerService?.playPendingIntent
        val playPause =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (pausePendingIntent != null) {
                    RemoteAction(
                        Icon.createWithResource(
                            this@PlayerActivity,
                            androidx.media3.ui.R.drawable.exo_icon_pause,
                        ),
                        "Pause",
                        "Pause",
                        pausePendingIntent,
                    )
                } else if (playPendingIntent != null) {
                    RemoteAction(
                        Icon.createWithResource(
                            this@PlayerActivity,
                            androidx.media3.ui.R.drawable.exo_icon_play,
                        ),
                        "Play",
                        "Play",
                        playPendingIntent,
                    )
                } else {
                    null
                }
            } else {
                null
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setActions(
                        listOfNotNull(
                            prev,
                            playPause,
                            next,
                        ),
                    )
                    .setSourceRectHint(rect)
                    .setAutoEnterEnabled(true)
                    .build(),
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setSourceRectHint(rect)
                    .build(),
            )
        }
    }

    private var rect: Rect? = null
        set(value) {
            field = value
            updatePictureInPictureParams()
        }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPip()
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setSourceRectHint(rect)
                    .build(),
            )
        } else {
            this.enterPictureInPictureMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        }
        if (isInPictureInPictureMode) {
            this.viewModel.areControlsVisible.value = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        this.intent = intent
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }

        val props = parseProps(intent.extras)
        if (this.lastProps?.playerProps?.media != props.playerProps.media) {
            this.lastProps = props
            props.let {
                viewModel =
                    PlayerViewModel(
                        this,
                        it.serverUrl,
                    ).apply {
                        startPlayerService(
                            PlayerServiceProps.Start(
                                it.playerProps,
                                it.serverUrl,
                                it.interruptService,
                            ),
                        )
                    }
            }

            onBackPressedDispatcher.addCallback {
                enterPip()
                startActivity(
                    Intent(this@PlayerActivity, BrowserActivity::class.java).apply {
                        putExtra(
                            BrowserActivity.BROWSER_STATE,
                            Json.encodeToString(playerService?.browserState ?: emptyList()),
                        )
                    },
                )
            }

            setContent {
                val service by PlayerService.instance.collectAsState()
                service?.let { serviceInterface ->
                    val isPlaying by serviceInterface.isPlayingState.collectAsState()
                    LaunchedEffect(isPlaying) {
                        updatePictureInPictureParams()
                        if (isPlaying) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    PlayerWrapper(viewModel, serviceInterface, {
                        rect = it
                    }) { updatePictureInPictureParams() }
                } ?: CircularProgressIndicator()
            }
            if (playerService?.isPlayingState?.value == true) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    companion object {
        const val PLAYER_ACTIVITY_PROPS_EXTRA_NAME = "PlayerActivityProps"

        private fun parseProps(savedInstanceState: Bundle?): PlayerActivityProps =
            Json.decodeFromString<PlayerActivityProps>(
                requireNotNull(
                    savedInstanceState?.getString(PLAYER_ACTIVITY_PROPS_EXTRA_NAME)
                        .also { Log.d("PlayerActivity", "$it") },
                ) {
                    "Cannot Start PlayerActivity without $PLAYER_ACTIVITY_PROPS_EXTRA_NAME"
                },
            )
    }
}

@Composable
fun PlayerWrapper(
    viewModel: PlayerViewModel,
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
    val nullableMedia by serviceInterface.media.collectAsState()
    val isLoading by serviceInterface.isLoading.collectAsState()
    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.background(Color.Transparent))
    }
    nullableMedia?.let { media ->
        Log.d("PlayerActivity", media.toString())
        LaunchedEffect(key1 = media, key2 = updateMedia) {
            updateMedia()
        }
        val prev by serviceInterface.prev.collectAsState()
        val next by serviceInterface.next.collectAsState()
        val isPlaying by serviceInterface.isPlayingState.collectAsState()
        val bufferedPosition by serviceInterface.bufferedPosition.collectAsState()
        val currentPlayerTime by serviceInterface.currentPlayerTime.collectAsState()
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidView(
                modifier =
                    Modifier.clickable {
                        viewModel.setAreControlsVisible(areControlsVisible.not())
                    }.background(Color.Black).fillMaxHeight().fillMaxWidth(),
                factory = { context ->
                    serviceInterface.getPlayerView(context).apply {
                        useController = false
                        this.addOnLayoutChangeListener { view: View, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
                            view.useRect()
                        }
                        this.useRect()
                    }
                },
            )
            PlayerControls(
                modifier = Modifier.fillMaxSize(),
                isVisible = areControlsVisible,
                isPlaying = isPlaying,
                title = media.details,
                onPrev =
                    if (prev == null) {
                        null
                    } else {
                        { serviceInterface.playPrev() }
                    },
                onNext =
                    if (next == null) {
                        null
                    } else {
                        { serviceInterface.playNext() }
                    },
                onPlay = { serviceInterface.play() },
                onPause = { serviceInterface.pause() },
                onSeek = { serviceInterface.seek(it) },
                bufferedPosition = media.startOffset + bufferedPosition,
                currentPlayerTime = media.startOffset + currentPlayerTime,
                runTime = media.runTime,
                onAudioChanged = {
                    MainScope().launch {
                        serviceInterface.loadMedia(
                            media.copy(
                                audioTrackName = it,
                                startOffset = media.startOffset + currentPlayerTime,
                            ),
                        )
                    }
                },
                onVideoChanged = {
                    MainScope().launch {
                        serviceInterface.loadMedia(
                            media.copy(
                                videoTrackName = it,
                                startOffset = media.startOffset + currentPlayerTime,
                            ),
                        )
                    }
                },
                onSubtitleChanged = {
                    MainScope().launch {
                        serviceInterface.loadMedia(
                            media.copy(
                                subtitleTrackName = it,
                                startOffset = media.startOffset + currentPlayerTime,
                            ),
                        )
                    }
                },
                currentMedia = media,
                onNormalizationChanged = {
                    MainScope().launch {
                        serviceInterface.loadMedia(
                            media.copy(normalizationMethod = it, startOffset = media.startOffset + currentPlayerTime),
                        )
                    }
                },
            )
        }
    }
}
