package io.rewynd.android.browser

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import io.rewynd.android.browser.component.BrowserRouter
import io.rewynd.android.model.PlayerMedia
import io.rewynd.android.player.PlayerActivity
import io.rewynd.android.player.PlayerActivityAction
import io.rewynd.android.player.PlayerActivityProps
import io.rewynd.android.player.PlayerProps
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BrowserActivity : AppCompatActivity() {
    private val viewModel by lazy { BrowserViewModel(application) }

    override fun onResume() {
        super.onResume()
        setContent {
            val navController = rememberNavController()
            val state = intent.getBundleExtra(BROWSER_STATE)
            navController.restoreState(state)
            MaterialTheme(colorScheme = darkColorScheme()) {
                BrowserRouter(
                    navController,
                    BrowserNavigationActions(navController),
                    viewModel,
                    mkStartPlayerHandler(navController),
                )
            }
        }
    }

    private fun mkStartPlayerHandler(navController: NavController): (PlayerMedia) -> Unit =
        {
            startActivity(
                Intent(this, PlayerActivity::class.java).apply {
                    putExtra(
                        PlayerActivity.PLAYER_ACTIVITY_ACTION_KEY,
                        Json.encodeToString<PlayerActivityAction>(
                            PlayerActivityAction.Start(
                                PlayerActivityProps(
                                    PlayerProps(it),
                                    interruptService = true,
                                ),
                            ),
                        ),
                    )
                    putExtra(BROWSER_STATE, navController.saveState())
                },
            )
            finish()
        }

    companion object {
        const val BROWSER_STATE = "BrowserState"
    }
}
