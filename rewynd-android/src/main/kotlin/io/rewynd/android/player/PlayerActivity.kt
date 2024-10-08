package io.rewynd.android.player

import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.rewynd.android.browser.BrowserActivity
import io.rewynd.android.browser.BrowserActivity.Companion.BROWSER_STATE
import io.rewynd.android.component.player.PlayerWrapper
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

@Serializable
sealed interface PlayerActivityAction {
    @Serializable
    data class Start(val props: PlayerActivityProps) : PlayerActivityAction

    @Serializable
    data object Stop : PlayerActivityAction
}

class PlayerActivity : AppCompatActivity() {
    private var lastProps: PlayerActivityProps? = null
    private lateinit var viewModel: PlayerViewModel

    private val playerService: PlayerServiceInterface?
        get() = PlayerService.instance.value

    private fun updatePictureInPictureParams() {
        val state = playerService?.getState()
        val next =
            if (state?.next != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                RemoteAction(
                    Icon.createWithResource(
                        this@PlayerActivity,
                        androidx.media3.ui.R.drawable.exo_icon_next,
                    ),
                    "Next",
                    "Next",
                    mkPlayerServiceIntent(PlayerServiceProps.Next),
                )
            } else {
                null
            }

        val prev =
            if (state?.next != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                RemoteAction(
                    Icon.createWithResource(
                        this@PlayerActivity,
                        androidx.media3.ui.R.drawable.exo_icon_previous,
                    ),
                    "Prev",
                    "Prev",
                    mkPlayerServiceIntent(PlayerServiceProps.Prev),
                )
            } else {
                null
            }

        val playPause =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (state?.isPlaying == true) {
                    RemoteAction(
                        Icon.createWithResource(
                            this@PlayerActivity,
                            androidx.media3.ui.R.drawable.exo_icon_pause,
                        ),
                        "Pause",
                        "Pause",
                        mkPlayerServiceIntent(PlayerServiceProps.Pause),
                    )
                } else {
                    RemoteAction(
                        Icon.createWithResource(
                            this@PlayerActivity,
                            androidx.media3.ui.R.drawable.exo_icon_play,
                        ),
                        "Play",
                        "Play",
                        mkPlayerServiceIntent(PlayerServiceProps.Play),
                    )
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
            this.viewModel.setControlsVisible(false)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
    }

    override fun onResume() {
        super.onResume()
        when (
            val action = this.intent.getStringExtra(
                PLAYER_ACTIVITY_ACTION_KEY
            ).let {
                Json.decodeFromString<PlayerActivityAction>(
                    requireNotNull(it) { "PlayerActivityAction must be set to instantiate PlayerActivity" }
                )
            }
        ) {
            is PlayerActivityAction.Start -> {
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

                val props = action.props
                val browserState = intent.getBundleExtra(BROWSER_STATE)
                if (this.lastProps?.playerProps?.media != props.playerProps.media) {
                    this.lastProps = props
                    props.let {
                        viewModel =
                            PlayerViewModel(
                                application,
                            ).apply {
                                startPlayerService(
                                    PlayerServiceProps.Start(
                                        it.playerProps,
                                        it.interruptService,
                                    ),
                                    browserState
                                )
                            }
                    }

                    onBackPressedDispatcher.addCallback {
                        enterPip()
                        startActivity(
                            Intent(this@PlayerActivity, BrowserActivity::class.java).apply {
                                putExtra(
                                    BROWSER_STATE,
                                    playerService?.browserState,
                                )
                            },
                        )
                    }

                    setContent {
                        val service by PlayerService.instance.collectAsStateWithLifecycle()
                        service?.let { serviceInterface ->
                            val playerState by serviceInterface.playerState
                                .collectAsStateWithLifecycle(PlayerState.DEFAULT)

                            LaunchedEffect(playerState.isPlaying, playerState.media) {
                                updatePictureInPictureParams()
                                if (playerState.isPlaying) {
                                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    while (true) {
                                        delay(1.seconds)
                                        serviceInterface.getState()
                                    }
                                } else {
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                }
                            }

                            PlayerWrapper(
                                viewModel,
                                playerState,
                                serviceInterface,
                                {
                                    rect = it
                                }
                            ) { updatePictureInPictureParams() }
                        } ?: CircularProgressIndicator(Modifier.fillMaxSize())
                    }
                    if (playerService?.getState()?.isPlaying == true) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }

            is PlayerActivityAction.Stop -> {
                this.intent.getBundleExtra(BROWSER_STATE)?.let {
                    startActivity(
                        Intent(this, BrowserActivity::class.java).apply {
                            putExtra(BROWSER_STATE, it)
                        }
                    )
                }
                this.finishAndRemoveTask()
            }
        }
    }

    companion object {
        const val PLAYER_ACTIVITY_ACTION_KEY = "PlayerActivityAction"
    }
}
