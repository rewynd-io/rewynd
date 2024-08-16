package io.rewynd.android.browser

import androidx.navigation.NavHostController
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Library
import io.rewynd.model.MovieInfo
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo

interface IBrowserNavigationActions {
    fun home()
    fun search()
    fun episode(episodeInfo: EpisodeInfo)
    fun library(library: Library)
    fun show(showInfo: ShowInfo)
    fun movie(movieInfo: MovieInfo)
    fun season(seasonInfo: SeasonInfo)
    fun wrap(wrapper: (BrowserState) -> Unit): IBrowserNavigationActions = object : IBrowserNavigationActions {
        override fun home() {
            wrapper(BrowserState.HomeState)
            this@IBrowserNavigationActions.home()
        }

        override fun search() {
            wrapper(BrowserState.SearchState)
            this@IBrowserNavigationActions.search()
        }

        override fun episode(episodeInfo: EpisodeInfo) {
            wrapper(BrowserState.EpisodeState(episodeInfo))
            this@IBrowserNavigationActions.episode(episodeInfo)
        }

        override fun library(library: Library) {
            wrapper(BrowserState.LibraryState(library))
            this@IBrowserNavigationActions.library(library)
        }

        override fun movie(movieInfo: MovieInfo) {
            wrapper(BrowserState.MovieState(movieInfo))
            this@IBrowserNavigationActions.movie(movieInfo)
        }

        override fun show(showInfo: ShowInfo) {
            wrapper(BrowserState.ShowState(showInfo))
            this@IBrowserNavigationActions.show(showInfo)
        }

        override fun season(seasonInfo: SeasonInfo) {
            wrapper(BrowserState.SeasonState(seasonInfo))
            this@IBrowserNavigationActions.season(seasonInfo)
        }
    }
}

class BrowserNavigationActions(private val navController: NavHostController) : IBrowserNavigationActions {
    override fun home() = navController.navigate(BrowserState.HomeState) {
        popUpTo(BrowserState.HomeState)
        launchSingleTop = true
    }

    override fun search() = navController.navigate(BrowserState.SearchState) {
        popUpTo(BrowserState.SearchState)
        launchSingleTop = true
    }

    override fun episode(episodeInfo: EpisodeInfo) = BrowserState.EpisodeState(episodeInfo).let {
        navController.navigate(it) {
            popUpTo(it)
        }
    }

    override fun library(library: Library) = BrowserState.LibraryState(library).let {
        navController.navigate(it) {
            popUpTo(it)
        }
    }

    override fun movie(movieInfo: MovieInfo) = BrowserState.MovieState(movieInfo).let {
        navController.navigate(it) {
            popUpTo(it)
        }
    }

    override fun show(showInfo: ShowInfo) = BrowserState.ShowState(showInfo).let {
        navController.navigate(it) {
            popUpTo(it)
        }
    }

    override fun season(seasonInfo: SeasonInfo) = BrowserState.SeasonState(seasonInfo).let {
        navController.navigate(it) {
            popUpTo(it)
        }
    }
}
