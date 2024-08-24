package io.rewynd.android.browser

import androidx.navigation.NavHostController

class BrowserNavigationActions(private val navController: NavHostController) {
    fun home() = navController.navigate(BrowserState.HomeState) {
        popUpTo(BrowserState.HomeState) {
            inclusive = true
        }
        launchSingleTop = true
    }

    fun settings() = navController.navigate(BrowserState.SettingsState) {
        popUpTo(BrowserState.HomeState) {
            inclusive = true
        }
        launchSingleTop = true
    }

    fun search() = navController.navigate(BrowserState.SearchState) {
        popUpTo(BrowserState.SearchState) {
            inclusive = true
        }
        launchSingleTop = true
    }

    fun episode(id: String) = BrowserState.EpisodeState(id).let {
        navController.navigate(it) {
            popUpTo<BrowserState.EpisodeState> {
                inclusive = true
            }
        }
    }

    fun library(id: String) = BrowserState.LibraryState(id).let {
        navController.navigate(it) {
            popUpTo<BrowserState.LibraryState> {
                inclusive = true
            }
        }
    }

    fun movie(id: String) = BrowserState.MovieState(id).let {
        navController.navigate(it) {
            popUpTo<BrowserState.MovieState> {
                inclusive = true
            }
        }
    }

    fun show(id: String) = BrowserState.ShowState(id).let {
        navController.navigate(it) {
            popUpTo<BrowserState.ShowState> {
                inclusive = true
            }
        }
    }

    fun season(id: String) = BrowserState.SeasonState(id).let {
        navController.navigate(it) {
            popUpTo<BrowserState.SeasonState> {
                inclusive = true
            }
        }
    }
}
