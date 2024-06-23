package io.rewynd.android.player

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.android.player.PlayerService.Companion.PLAYER_SERVICE_INTENT_BUNDLE_BROWSER_STATE_KEY
import io.rewynd.android.player.PlayerService.Companion.PLAYER_SERVICE_INTENT_BUNDLE_PROPS_KEY
import io.rewynd.client.RewyndClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PlayerViewModel(
    private val activity: PlayerActivity,
    private val serverUrl: ServerUrl,
    val client: RewyndClient = mkRewyndClient(serverUrl),
) : AndroidViewModel(activity.application) {
    fun startPlayerService(serviceProps: PlayerServiceProps, browserState: Bundle?) {
        val intent =
            Intent(activity.application, PlayerService::class.java).apply {
                putExtra(PLAYER_SERVICE_INTENT_BUNDLE_PROPS_KEY, Json.encodeToString(serviceProps))
                putExtra(PLAYER_SERVICE_INTENT_BUNDLE_BROWSER_STATE_KEY, browserState)
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.application.startForegroundService(intent)
        } else {
            activity.application.startService(intent)
        }
    }

    val areControlsVisible = MutableStateFlow(false)

    fun setAreControlsVisible(state: Boolean) =
        runBlocking {
            areControlsVisible.emit(state)
        }
}
