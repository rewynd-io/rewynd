package io.rewynd.android.browser.component

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.rewynd.android.browser.BrowserState
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.component.ApiImage
import io.rewynd.android.model.PlayerMedia
import io.rewynd.android.player.PlayerActivity
import io.rewynd.android.player.PlayerActivityProps
import io.rewynd.android.player.PlayerProps
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Progress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeBrowser(
    nextEpisode: EpisodeInfo?,
    previousEpisode: EpisodeInfo?,
    progress: Progress?,
    episodeInfo: EpisodeInfo,
    backStack: ImmutableList<BrowserState>,
    serverUrl: ServerUrl,
    modifier: Modifier = Modifier,
    loadImage: suspend (String) -> Bitmap?,
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        val context = LocalContext.current
        Card(onClick = {
            context.startActivity(
                Intent(context, PlayerActivity::class.java).apply {
                    putExtra(
                        PlayerActivity.PLAYER_ACTIVITY_PROPS_EXTRA_NAME,
                        Json.encodeToString(
                            PlayerActivityProps(
                                PlayerProps(
                                    PlayerMedia.Episode(
                                        PlayerMedia.Episode.EpisodePlaybackMethod.Sequential,
                                        episodeInfo,
                                        runTime = episodeInfo.runTime.seconds,
                                        startOffset =
                                            episodeInfo.runTime.seconds.times(
                                                (progress?.percent ?: 0.0),
                                            ),
                                        videoTrackName = episodeInfo.videoTracks.keys.firstOrNull(),
                                        audioTrackName = episodeInfo.audioTracks.keys.firstOrNull(),
                                        subtitleTrackName = episodeInfo.subtitleTracks.keys.firstOrNull(),
                                        // TODO load normalization method from user prefs
                                        normalizationMethod = null,
                                    ),
                                    backStack,
                                ),
                                serverUrl = serverUrl,
                                interruptService = true,
                            ),
                        ),
                    )
                },
            )
        }) {
            ApiImage(episodeInfo.episodeImageId, loadImage = loadImage)
        }
        Text(episodeInfo.title, color = Color.White)
        Text("Season ${episodeInfo.season} Episode: ${episodeInfo.episode}", color = Color.White)
        (episodeInfo.plot ?: episodeInfo.outline)?.let { Text(it, color = Color.White) }
        Text("Rating: ${episodeInfo.rating}", color = Color.White)
    }
}
