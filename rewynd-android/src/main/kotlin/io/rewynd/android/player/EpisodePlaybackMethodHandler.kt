package io.rewynd.android.player

import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.android.model.PlayerMedia
import io.rewynd.client.RewyndClient
import io.rewynd.client.result
import io.rewynd.model.GetNextEpisodeRequest
import io.rewynd.model.SortOrder
import kotlin.time.Duration.Companion.seconds

object EpisodePlaybackMethodHandler {
    private val log by lazy { KotlinLogging.logger { } }

    suspend fun next(
        client: RewyndClient,
        playerMedia: PlayerMedia.Episode,
    ): PlayerMedia.Episode? =
        try {
            callGetNextEpisode(client, playerMedia, SortOrder.Ascending)
        } catch (e: Exception) {
            log.error(e) { "Failed to find next episode for $playerMedia" }
            null
        }

    suspend fun prev(
        client: RewyndClient,
        playerMedia: PlayerMedia.Episode,
    ): PlayerMedia.Episode? =
        try {
            callGetNextEpisode(client, playerMedia, SortOrder.Descending)
        } catch (e: Exception) {
            log.error(e) { "Failed to find previous episode for $playerMedia" }
            null
        }

    private suspend fun callGetNextEpisode(
        client: RewyndClient,
        playerMedia: PlayerMedia.Episode,
        order: SortOrder,
    ) = client.getNextEpisode(GetNextEpisodeRequest(playerMedia.info.id, order))
        .result().getOrNull()?.episodeInfo?.let { episodeInfo ->
            PlayerMedia.Episode(
                playbackMethod = playerMedia.playbackMethod,
                info = episodeInfo,
                runTime = episodeInfo.runTime.seconds,
                startOffset = (episodeInfo.progress.percent * episodeInfo.runTime).seconds,
                videoTrackName = playerMedia.videoTrackName?.let { episodeInfo.videoTracks.keys.firstOrNull() },
                audioTrackName = playerMedia.audioTrackName?.let { episodeInfo.audioTracks.keys.firstOrNull() },
                subtitleTrackName =
                playerMedia.subtitleTrackName?.let {
                    episodeInfo.subtitleTracks.keys.firstOrNull()
                },
                normalizationMethod = playerMedia.normalizationMethod,
            )
        }
}
