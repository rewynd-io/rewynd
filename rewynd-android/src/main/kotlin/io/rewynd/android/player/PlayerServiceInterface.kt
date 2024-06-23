package io.rewynd.android.player

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import androidx.media3.ui.PlayerView
import io.rewynd.android.model.PlayerMedia
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface PlayerServiceInterface {
    val browserState: Bundle?

    val isLoading: StateFlow<Boolean>
    val isPlayingState: StateFlow<Boolean>
    val media: StateFlow<PlayerMedia?>
    val next: StateFlow<PlayerMedia?>
    val prev: StateFlow<PlayerMedia?>
    val bufferedPosition: StateFlow<Duration>
    val actualStartOffset: StateFlow<Duration>
    val currentPlayerTime: StateFlow<Duration>
    val nextPendingIntent: PendingIntent?
    val prevPendingIntent: PendingIntent?
    val pausePendingIntent: PendingIntent?
    val playPendingIntent: PendingIntent?

    fun getPlayerView(context: Context): PlayerView

    fun playNext()

    fun playPrev()

    fun stop()

    fun pause()

    fun play()

    fun seek(desired: Duration)

    suspend fun loadMedia(mediaInfo: PlayerMedia)
}
