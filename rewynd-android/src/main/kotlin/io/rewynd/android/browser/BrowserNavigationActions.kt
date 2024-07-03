package io.rewynd.android.browser

import androidx.navigation.NavHostController
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Library
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo

class BrowserNavigationActions(private val navController: NavHostController) {
    fun home() = navController.navigate(BrowserState.HomeState) {
        popUpTo(BrowserState.HomeState) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

    fun episode(episodeInfo: EpisodeInfo) = BrowserState.EpisodeState(episodeInfo).let {
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