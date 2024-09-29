package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.rewynd.android.browser.BrowserNavigationActions
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.component.ApiImage
import io.rewynd.android.model.PlayerMedia
import io.rewynd.android.util.details
import kotlin.time.Duration.Companion.seconds

@Composable
fun EpisodeBrowser(
    episodeId: String,
    viewModel: BrowserViewModel,
    startPlayer: (PlayerMedia) -> Unit,
    actions: BrowserNavigationActions,
    modifier: Modifier = Modifier
) = with(viewModel) {
    loadEpisode(episodeId)
    loadNextEpisode(episodeId)
    loadPrevEpisode(episodeId)

    episode?.let { episodeInfo ->
        loadSeason(episodeInfo.seasonId)
        loadShow(episodeInfo.showId)

        Column(modifier.verticalScroll(rememberScrollState())) {
            Row {
                Button({
                    actions.library(episodeInfo.libraryId)
                }) {
                    Text(episodeInfo.libraryId, color = Color.White)
                }
                Button({
                    show?.let {
                        actions.show(it.id)
                    }
                }) {
                    episodeInfo.showName.let { Text(it, color = Color.White) }
                }
                Button({
                    season?.let {
                        actions.season(it.id)
                    }
                }) {
                    Text("Season ${episodeInfo.season}", color = Color.White)
                }
                Text("Episode: ${episodeInfo.episode}", color = Color.White)
            }
            Text(episodeInfo.title, color = Color.White)

            Card(onClick = {
                startPlayer(
                    PlayerMedia.Episode(
                        PlayerMedia.Episode.EpisodePlaybackMethod.Sequential,
                        episodeInfo,
                        runTime = episodeInfo.runTime.seconds,
                        startOffset =
                        episodeInfo.runTime.seconds.times(
                            (episodeInfo.progress.percent),
                        ),
                        videoTrackName = episodeInfo.videoTracks.keys.firstOrNull(),
                        audioTrackName = episodeInfo.audioTracks.keys.firstOrNull(),
                        subtitleTrackName = episodeInfo.subtitleTracks.keys.firstOrNull(),
                        // TODO load normalization method from user prefs
                        normalizationMethod = null,
                    ),
                )
            }) {
                ApiImage(episodeInfo.episodeImageId, loadImage = viewModel.imageLoader)
            }
            (episodeInfo.plot ?: episodeInfo.outline)?.let { Text(it, color = Color.White) }
            Text("Rating: ${episodeInfo.rating}", color = Color.White)
            BoxWithConstraints {
                val size = listOfNotNull(prevEpisode, nextEpisode).size
                Row {
                    prevEpisode?.let {
                        Card(
                            modifier = Modifier.width(this@BoxWithConstraints.maxWidth / size),
                            onClick = {
                                actions.episode(it.id)
                            }
                        ) {
                            Text("Previous Episode")
                            ApiImage(it.episodeImageId, loadImage = viewModel.imageLoader)
                            Text(it.details)
                        }
                    }
                    nextEpisode?.let {
                        Card(
                            modifier = Modifier.width(this@BoxWithConstraints.maxWidth / size),
                            onClick = {
                                actions.episode(it.id)
                            }
                        ) {
                            Text("Next Episode")
                            ApiImage(it.episodeImageId, loadImage = viewModel.imageLoader)
                            Text(it.details)
                        }
                    }
                }
            }
        }
    }
} ?: CircularProgressIndicator()
