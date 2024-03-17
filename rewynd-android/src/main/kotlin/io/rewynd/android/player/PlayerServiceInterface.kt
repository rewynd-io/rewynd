package io.rewynd.android.player

import android.app.PendingIntent
import androidx.media3.exoplayer.ExoPlayer
import io.rewynd.android.browser.BrowserState
import io.rewynd.android.model.PlayerMedia
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface PlayerServiceInterface {
    val browserState: List<BrowserState>
    val player: ExoPlayer
    val isLoading: StateFlow<Boolean>
    val isPlayingState: StateFlow<Boolean>
    val media: StateFlow<PlayerMedia?>
    val next: StateFlow<PlayerMedia?>
    val prev: StateFlow<PlayerMedia?>
    val bufferedPosition: StateFlow<Duration>
    val currentPlayerTime: StateFlow<Duration>
    val nextPendingIntent: PendingIntent?
    val prevPendingIntent: PendingIntent?
    val pausePendingIntent: PendingIntent?
    val playPendingIntent: PendingIntent?

    fun playNext()

    fun playPrev()

    fun stop()

    fun pause()

    fun play()

    fun seek(desired: Duration)

    suspend fun loadMedia(mediaInfo: PlayerMedia)
}
