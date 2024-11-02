package io.rewynd.android.browser.component

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.rewynd.android.browser.BrowserNavigationActions
import io.rewynd.android.browser.BrowserState
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.model.PlayerMedia

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // TODO use padding
@Suppress("ViewModelForwarding", "ModifierMissing")
@Composable
fun BrowserRouter(
    navController: NavHostController,
    actions: BrowserNavigationActions,
    viewModel: BrowserViewModel,
    startPlayer: (PlayerMedia) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = BrowserState.HomeState,
    ) {
        composable<BrowserState.SearchState> {
            SearchPage(viewModel, actions) { if (!it) navController.popBackStack() }
        }
        composable<BrowserState.HomeState> {
            BrowserWrapper(actions) {
                HomeBrowser(actions::library, actions::episode, viewModel)
            }
        }
        composable<BrowserState.SettingsState> {
            BrowserWrapper(actions) {
                SettingsBrowser(viewModel)
            }
        }
        composable<BrowserState.LibraryState> {
            val state = it.toRoute<BrowserState.LibraryState>()
            BrowserWrapper(actions) {
                LibraryBrowser(state.id, actions, viewModel)
            }
        }
        composable<BrowserState.ShowState> {
            val state = it.toRoute<BrowserState.ShowState>()
            BrowserWrapper(actions) {
                ShowBrowser(state.id, viewModel, actions)
            }
        }
        composable<BrowserState.MovieState> {
            val state = it.toRoute<BrowserState.MovieState>()
            BrowserWrapper(actions) {
                MovieBrowser(state.id, viewModel, startPlayer, actions)
            }
        }
        composable<BrowserState.SeasonState> {
            val state = it.toRoute<BrowserState.SeasonState>()
            BrowserWrapper(actions) {
                SeasonBrowser(state.id, viewModel, actions)
            }
        }
        composable<BrowserState.EpisodeState> {
            val state = it.toRoute<BrowserState.EpisodeState>()
            BrowserWrapper(actions) {
                EpisodeBrowser(
                    episodeId = state.id,
                    viewModel = viewModel,
                    startPlayer = startPlayer,
                    actions = actions,
                    modifier = Modifier,
                )
            }
        }
    }
}
