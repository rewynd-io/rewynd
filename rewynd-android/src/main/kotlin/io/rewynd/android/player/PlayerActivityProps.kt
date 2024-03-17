package io.rewynd.android.player

import io.rewynd.android.client.ServerUrl

@kotlinx.serialization.Serializable
data class PlayerActivityProps(
    val playerProps: PlayerProps,
    val serverUrl: ServerUrl,
    val interruptService: Boolean = true,
)
