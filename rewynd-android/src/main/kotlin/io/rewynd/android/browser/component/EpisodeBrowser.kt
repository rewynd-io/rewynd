package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.rewynd.android.browser.BrowserViewModel
import io.rewynd.android.component.ApiImage
import io.rewynd.android.model.PlayerMedia
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Progress
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

@Composable
fun EpisodeBrowser(
    episodeInfo: EpisodeInfo,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    startPlayer: (PlayerMedia) -> Unit,
) {
    var progressState by remember { mutableStateOf<Progress?>(null) }
    LaunchedEffect(episodeInfo) {
        progressState = viewModel.getProgress(episodeInfo.id).filterNotNull().first()
    }

    progressState?.let { progress ->
        Column(modifier.verticalScroll(rememberScrollState())) {
            Card(onClick = {
                startPlayer(
                    PlayerMedia.Episode(
                        PlayerMedia.Episode.EpisodePlaybackMethod.Sequential,
                        episodeInfo,
                        runTime = episodeInfo.runTime.seconds,
                        startOffset =
                        episodeInfo.runTime.seconds.times(
                            (progress.percent),
                        ),
                        videoTrackName = episodeInfo.videoTracks.keys.firstOrNull(),
                        audioTrackName = episodeInfo.audioTracks.keys.firstOrNull(),
                        subtitleTrackName = episodeInfo.subtitleTracks.keys.firstOrNull(),
                        // TODO load normalization method from user prefs
                        normalizationMethod = null,
                    )
                )
            }) {
                ApiImage(episodeInfo.episodeImageId, loadImage = viewModel::loadImage)
            }
            Text(episodeInfo.title, color = Color.White)
            Text("Season ${episodeInfo.season} Episode: ${episodeInfo.episode}", color = Color.White)
            (episodeInfo.plot ?: episodeInfo.outline)?.let { Text(it, color = Color.White) }
            Text("Rating: ${episodeInfo.rating}", color = Color.White)
        }
    } ?: CircularProgressIndicator()
}
