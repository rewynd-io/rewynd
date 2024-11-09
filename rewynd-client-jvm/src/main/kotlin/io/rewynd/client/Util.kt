package io.rewynd.client

import io.rewynd.model.EpisodeInfo

fun EpisodeInfo.Companion.compare(a: EpisodeInfo, b: EpisodeInfo) = if (a.episode < b.episode) {
    -1
} else if (a.episode > b.episode) {
    1
} else {
    if (a.title < b.title) {
        -1
    } else if (a.title > b.title) {
        1
    } else {
        if (a.id < b.id) {
            -1
        } else if (a.id > b.id) {
            1
        } else {
            0
        }
    }
}