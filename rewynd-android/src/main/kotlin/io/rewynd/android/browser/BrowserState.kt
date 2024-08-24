package io.rewynd.android.browser

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
sealed interface BrowserState {
    @Serializable
    @Parcelize
    class EpisodeState(val id: String) :
        BrowserState,
        Parcelable

    @Serializable
    @Parcelize
    class SeasonState(val id: String) : BrowserState, Parcelable

    @Serializable
    @Parcelize
    class ShowState(val id: String) : BrowserState, Parcelable

    @Serializable
    @Parcelize
    class MovieState(val id: String) : BrowserState, Parcelable

    @Serializable
    @Parcelize
    class LibraryState(val id: String) : BrowserState, Parcelable

    @Serializable
    @Parcelize
    data object HomeState : BrowserState, Parcelable

    @Serializable
    @Parcelize
    data object SearchState : BrowserState, Parcelable
}
