package io.rewynd.android.player

import io.rewynd.android.model.PlayerMedia
import io.rewynd.client.RewyndClient

// TODO figure out the nice inheritance/sealed interface way of doing this
object PlaybackMethodHandler {
    suspend fun next(
        client: RewyndClient,
        playerMedia: PlayerMedia,
    ): PlayerMedia? =
        when (playerMedia) {
            is PlayerMedia.Episode -> EpisodePlaybackMethodHandler.next(client, playerMedia)
        }

    suspend fun prev(
        client: RewyndClient,
        playerMedia: PlayerMedia,
    ): PlayerMedia? =
        when (playerMedia) {
            is PlayerMedia.Episode -> EpisodePlaybackMethodHandler.prev(client, playerMedia)
        }
}
