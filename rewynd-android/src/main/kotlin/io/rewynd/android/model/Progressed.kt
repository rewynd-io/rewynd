package io.rewynd.android.model

import io.rewynd.model.Progress

data class Progressed<T>(val progress: Progress, val media: T)
