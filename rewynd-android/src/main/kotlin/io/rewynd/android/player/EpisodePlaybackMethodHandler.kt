package io.rewynd.android.player

import io.github.oshai.kotlinlogging.KotlinLogging
import io.rewynd.android.model.PlayerMedia
import io.rewynd.client.RewyndClient
import kotlin.time.Duration.Companion.seconds

object EpisodePlaybackMethodHandler {
    private val log by lazy { KotlinLogging.logger { } }

    suspend fun next(
        client: RewyndClient,
        playerMedia: PlayerMedia.Episode,
    ): PlayerMedia.Episode? =
        try {
            client.getNextEpisode(playerMedia.info.id).body().let { episodeInfo ->
                val progress = client.getUserProgress(episodeInfo.id).body().percent.takeIf { it < 0.95 } ?: 0.0
                PlayerMedia.Episode(
                    playbackMethod = playerMedia.playbackMethod,
                    info = episodeInfo,
                    runTime = episodeInfo.runTime.seconds,
                    startOffset = (progress * episodeInfo.runTime).seconds,
                    videoTrackName = episodeInfo.videoTracks.keys.firstOrNull(),
                    audioTrackName = episodeInfo.audioTracks.keys.firstOrNull(),
                    subtitleTrackName = episodeInfo.subtitleTracks.keys.firstOrNull(),
                    normalizationMethod = playerMedia.normalizationMethod,
                )
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to find next episode for $playerMedia" }
            null
        }

    suspend fun prev(
        client: RewyndClient,
        playerMedia: PlayerMedia.Episode,
    ): PlayerMedia.Episode? =
        try {
            client.getPreviousEpisode(playerMedia.info.id).body().let { episodeInfo ->
                val progress = client.getUserProgress(episodeInfo.id).body().percent.takeIf { it < 0.95 } ?: 0.0
                PlayerMedia.Episode(
                    playbackMethod = playerMedia.playbackMethod,
                    info = episodeInfo,
                    runTime = episodeInfo.runTime.seconds,
                    startOffset = (progress * episodeInfo.runTime).seconds,
                    videoTrackName = episodeInfo.videoTracks.keys.firstOrNull(),
                    audioTrackName = episodeInfo.audioTracks.keys.firstOrNull(),
                    subtitleTrackName = episodeInfo.subtitleTracks.keys.firstOrNull(),
                    normalizationMethod = playerMedia.normalizationMethod,
                )
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to find previous episode for $playerMedia" }
            null
        }
}
