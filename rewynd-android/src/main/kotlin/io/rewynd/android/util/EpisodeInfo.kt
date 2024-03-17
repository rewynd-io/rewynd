package io.rewynd.android.util

import io.rewynd.model.EpisodeInfo

val EpisodeInfo.details: String
    get() = "$showName - S${season?.toInt()}E${episode?.toInt()} - $title" // TODO proper formatting
