package io.rewynd.android.browser

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import io.rewynd.android.browser.component.BrowserRouter
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.login.MainViewModel
import io.rewynd.android.model.PlayerMedia
import io.rewynd.android.player.PlayerActivity
import io.rewynd.android.player.PlayerActivityAction
import io.rewynd.android.player.PlayerActivityProps
import io.rewynd.android.player.PlayerProps
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BrowserActivity : AppCompatActivity() {
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(application) }
    private val viewModel by lazy {
        BrowserViewModel(
            application,
            ServerUrl(
                requireNotNull(
                    prefs.getString(
                        MainViewModel.SERVER_URL,
                        null,
                    ),
                ) { "Missing Server Url preference!" },
            ),
        )
    }

    override fun onResume() {
        super.onResume()
        setContent {
            val navController = rememberNavController()
            val state = intent.getBundleExtra(BROWSER_STATE)
            navController.restoreState(state)
            BrowserRouter(
                navController,
                BrowserNavigationActions(navController),
                viewModel,
                mkStartPlayerHandler(navController)
            )
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
                                    serverUrl = viewModel.serverUrl,
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
