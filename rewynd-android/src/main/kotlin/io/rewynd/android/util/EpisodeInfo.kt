package io.rewynd.android.util

import io.rewynd.model.EpisodeInfo

val EpisodeInfo.details: String
    get() = "$showName - S${season}E$episode - $title" // TODO proper formatting
