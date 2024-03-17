package io.rewynd.android.player

import io.rewynd.android.browser.BrowserState
import io.rewynd.android.model.PlayerMedia

@kotlinx.serialization.Serializable
data class PlayerProps(val media: PlayerMedia, val browserState: List<BrowserState>)
