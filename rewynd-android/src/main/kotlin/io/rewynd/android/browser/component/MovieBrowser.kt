package io.rewynd.android.browser.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import kotlin.time.Duration.Companion.seconds

@Composable
fun MovieBrowser(
    movieId: String,
    viewModel: BrowserViewModel,
    startPlayer: (PlayerMedia) -> Unit,
    actions: BrowserNavigationActions,
    modifier: Modifier = Modifier
) = with(viewModel) {
    loadMovie(movieId)
    loadProgress(movieId)

    movie?.let { movieInfo ->
        progress?.let { progress ->
            Column(modifier.verticalScroll(rememberScrollState())) {
                Row {
                    Button({
                        library?.let {
                            actions.library(movieInfo.libraryId)
                        }
                    }) {
                        Text(movieInfo.libraryId, color = Color.White)
                    }
                }
                Text(movieInfo.title, color = Color.White)

                Card(onClick = {
                    startPlayer(
                        PlayerMedia.Movie(
                            movieInfo,
                            runTime = movieInfo.runTime.seconds,
                            startOffset =
                            movieInfo.runTime.seconds.times(
                                (progress.percent),
                            ),
                            videoTrackName = movieInfo.videoTracks.keys.firstOrNull(),
                            audioTrackName = movieInfo.audioTracks.keys.firstOrNull(),
                            subtitleTrackName = movieInfo.subtitleTracks.keys.firstOrNull(),
                            // TODO load normalization method from user prefs
                            normalizationMethod = null,
                        ),
                    )
                }) {
                    ApiImage(movieInfo.posterImageId, loadImage = viewModel.imageLoader)
                }
                (movieInfo.plot ?: movieInfo.outline)?.let { Text(it, color = Color.White) }
                Text("Rating: ${movieInfo.rating}", color = Color.White)
            }
        }
    } ?: CircularProgressIndicator()
}
