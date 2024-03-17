package io.rewynd.common.model

import kotlinx.serialization.Serializable

@Serializable
data class Mime(
    val mimeType: String,
    // RFC 6381
    val codecs: List<String>,
)
