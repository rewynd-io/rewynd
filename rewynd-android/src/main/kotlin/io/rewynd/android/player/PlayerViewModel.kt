package io.rewynd.android.player

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import arrow.atomic.Atomic
import io.rewynd.android.client.ServerUrl
import io.rewynd.android.client.mkRewyndClient
import io.rewynd.android.player.PlayerService.Companion.PLAYER_SERVICE_INTENT_BUNDLE_BROWSER_STATE_KEY
import io.rewynd.android.player.PlayerService.Companion.PLAYER_SERVICE_INTENT_BUNDLE_PROPS_KEY
import io.rewynd.client.RewyndClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

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

    val areControlsVisible: StateFlow<Boolean>
        get() = _areControlsVisible
    private val _areControlsVisible = MutableStateFlow(false)

    private val controlsTimeout = Atomic(Instant.DISTANT_PAST)
    fun setControlsVisible(visible: Boolean) {
        _areControlsVisible.value = visible
        if (visible) {
            controlsTimeout.updateAndGet {
                Instant.fromEpochMilliseconds(
                    max(
                        it.toEpochMilliseconds(),
                        (Clock.System.now() + 10.seconds).toEpochMilliseconds()
                    )
                )
            }
            viewModelScope.launch {
                delay(10.seconds)
                if (controlsTimeout.get() < Clock.System.now()) {
                    _areControlsVisible.value = false
                }
            }
        }
    }
}
