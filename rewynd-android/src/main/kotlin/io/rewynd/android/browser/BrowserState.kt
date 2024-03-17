package io.rewynd.android.browser

import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Library
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo

@kotlinx.serialization.Serializable
sealed interface BrowserState {
    @kotlinx.serialization.Serializable
    class EpisodeState(val episodeInfo: EpisodeInfo) : BrowserState

    @kotlinx.serialization.Serializable
    class SeasonState(val seasonInfo: SeasonInfo) : BrowserState

    @kotlinx.serialization.Serializable
    class ShowState(val showInfo: ShowInfo) : BrowserState

    @kotlinx.serialization.Serializable
    class LibraryState(val library: Library) : BrowserState

    @kotlinx.serialization.Serializable
    object HomeState : BrowserState
}
