package io.rewynd.android.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.media3.ui.PlayerView
import io.rewynd.android.model.PlayerMedia
import io.rewynd.android.player.PlayerService.Companion.PLAYER_SERVICE_INTENT_BUNDLE_PROPS_KEY
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration

interface PlayerServiceInterface {
    val browserState: Bundle?
    val playerState: StateFlow<PlayerState>

    fun getCurrentPosition(): Duration
    fun getPlayerView(context: Context): PlayerView

    fun playNext()

    fun playPrev()

    fun stop()

    fun pause()

    fun play()

    fun seek(desired: Duration)

    suspend fun loadMedia(mediaInfo: PlayerMedia)
}

fun Context.mkPlayerServiceIntent(props: PlayerServiceProps): PendingIntent =
    Intent(
        this,
        PlayerService::class.java,
    ).apply {
        putExtra(
            PLAYER_SERVICE_INTENT_BUNDLE_PROPS_KEY,
            Json.encodeToString(props),
        )
    }.let {
        PendingIntent.getService(
            this,
            props.requestCode,
            it,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
