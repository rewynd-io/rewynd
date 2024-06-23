package io.rewynd.android.browser.component

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.rewynd.android.browser.BrowserState
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.browser.parcelableType
import io.rewynd.android.model.PlayerMedia
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Library
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo
import kotlin.reflect.typeOf

@Suppress("ViewModelForwarding")
@Composable
fun BrowserRouter(
    navController: NavHostController,
    viewModel: BrowserViewModel,
    startPlayer: (PlayerMedia) -> Unit
) {
    val actions = BrowserNavigationActions(navController)
    Log.i("CURRENT_BROWSER_STATE", "${navController.saveState()}")
    Log.i("REWYND_COMPOSITION", "Recomposing NavHost")
    NavHost(
        navController = navController,
        startDestination = navController.currentDestination ?: BrowserState.HomeState
    ) {
        composable<BrowserState.HomeState> {
            HomeBrowser(actions::library, actions::episode, viewModel)
        }
        composable<BrowserState.LibraryState>(
            typeMap = mapOf(typeOf<Library>() to parcelableType<Library>())
        ) {
            val state = it.toRoute<BrowserState.LibraryState>()
            LibraryBrowser(state.library.name, actions::show, viewModel)
        }
        composable<BrowserState.ShowState>(
            typeMap = mapOf(typeOf<ShowInfo>() to parcelableType<ShowInfo>())
        ) {
            Log.i("REWYND_COMPOSITION", "Recomposing Show")
            val state = it.toRoute<BrowserState.ShowState>()
            ShowBrowser(state.showInfo, viewModel, actions::season)
        }
        composable<BrowserState.SeasonState>(
            typeMap = mapOf(typeOf<SeasonInfo>() to parcelableType<SeasonInfo>())
        ) {
            val state = it.toRoute<BrowserState.SeasonState>()
            SeasonBrowser(state.seasonInfo, viewModel, actions::episode)
        }
        composable<BrowserState.EpisodeState>(
            typeMap = mapOf(typeOf<EpisodeInfo>() to parcelableType<EpisodeInfo>())
        ) {
            val state = it.toRoute<BrowserState.EpisodeState>()
            EpisodeBrowser(state.episodeInfo, viewModel, startPlayer = startPlayer)
        }
    }
}

class BrowserNavigationActions(val navController: NavHostController) {
    fun home() = navController.navigate(BrowserState.HomeState) {
        popUpTo(BrowserState.HomeState) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

    fun episode(episodeInfo: EpisodeInfo) = BrowserState.EpisodeState(episodeInfo).let {
        Log.i("REWYND_NAVIGATION", "Navigating to Episode $episodeInfo")
        navController.navigate(it) {
            popUpTo(it) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun library(library: Library) = BrowserState.LibraryState(library).let {
        navController.navigate(it) {
            popUpTo(it) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun show(showInfo: ShowInfo) = BrowserState.ShowState(showInfo).let {
        Log.i("REWYND_NAVIGATION", "Navigating to show $showInfo")
        navController.navigate(it) {
            popUpTo(it) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun season(seasonInfo: SeasonInfo) = BrowserState.SeasonState(seasonInfo).let {
        navController.navigate(it) {
            popUpTo(it) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}
