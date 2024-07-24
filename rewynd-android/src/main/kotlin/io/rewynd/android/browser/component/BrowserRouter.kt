package io.rewynd.android.browser.component

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.rewynd.android.browser.BrowserState
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.browser.IBrowserNavigationActions
import io.rewynd.android.browser.parcelableType
import io.rewynd.android.model.PlayerMedia
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Library
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo
import kotlin.reflect.typeOf

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // TODO use padding
@Suppress("ViewModelForwarding", "ModifierMissing")
@Composable
fun BrowserRouter(
    navController: NavHostController,
    actions: IBrowserNavigationActions,
    viewModel: BrowserViewModel,
    startPlayer: (PlayerMedia) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = navController.currentDestination ?: BrowserState.HomeState,
    ) {
        composable<BrowserState.SearchState> {
            SearchPage(viewModel, actions) { if (!it) navController.popBackStack() }
        }
        composable<BrowserState.HomeState> {
            BrowserWrapper(actions) {
                HomeBrowser(actions::library, actions::episode, viewModel)
            }
        }
        composable<BrowserState.LibraryState>(
            typeMap = mapOf(typeOf<Library>() to parcelableType<Library>()),
        ) {
            val state = it.toRoute<BrowserState.LibraryState>()
            BrowserWrapper(actions) {
                LibraryBrowser(state.library.name, actions::show, viewModel)
            }
        }
        composable<BrowserState.ShowState>(
            typeMap = mapOf(typeOf<ShowInfo>() to parcelableType<ShowInfo>()),
        ) {
            val state = it.toRoute<BrowserState.ShowState>()
            BrowserWrapper(actions) {
                ShowBrowser(state.showInfo, viewModel, actions)
            }
        }
        composable<BrowserState.SeasonState>(
            typeMap = mapOf(typeOf<SeasonInfo>() to parcelableType<SeasonInfo>()),
        ) {
            val state = it.toRoute<BrowserState.SeasonState>()
            BrowserWrapper(actions) {
                SeasonBrowser(state.seasonInfo, viewModel, actions)
            }
        }
        composable<BrowserState.EpisodeState>(
            typeMap = mapOf(typeOf<EpisodeInfo>() to parcelableType<EpisodeInfo>()),
        ) {
            val state = it.toRoute<BrowserState.EpisodeState>()
            BrowserWrapper(actions) {
                EpisodeBrowser(
                    episodeInfo = state.episodeInfo,
                    viewModel = viewModel,
                    startPlayer = startPlayer,
                    actions = actions,
                    modifier = Modifier,
                )
            }
        }
    }
}
