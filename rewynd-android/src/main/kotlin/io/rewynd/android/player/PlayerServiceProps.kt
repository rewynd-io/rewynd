package io.rewynd.android.player

import io.rewynd.android.client.ServerUrl

@kotlinx.serialization.Serializable
sealed interface PlayerServiceProps {
    @kotlinx.serialization.Serializable
    data class Start(
        val playerProps: PlayerProps,
        val serverUrl: ServerUrl,
        val interruptPlayback: Boolean = true,
    ) : PlayerServiceProps

    @kotlinx.serialization.Serializable
    object Pause : PlayerServiceProps

    @kotlinx.serialization.Serializable
    object Play : PlayerServiceProps

    @kotlinx.serialization.Serializable
    object Next : PlayerServiceProps

    @kotlinx.serialization.Serializable
    object Prev : PlayerServiceProps

    @kotlinx.serialization.Serializable
    object Stop : PlayerServiceProps

    @Suppress("MagicNumber")
    val requestCode: Int
        get() =
            when (this) {
                is Start -> 0
                is Stop -> 1
                is Pause -> 2
                is Play -> 3
                is Prev -> 4
                is Next -> 5
            }
}
