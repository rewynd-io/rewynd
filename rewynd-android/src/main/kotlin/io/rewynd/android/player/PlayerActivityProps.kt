package io.rewynd.android.player

@kotlinx.serialization.Serializable
data class PlayerActivityProps(
    val playerProps: PlayerProps,
    val interruptService: Boolean = true,
)
