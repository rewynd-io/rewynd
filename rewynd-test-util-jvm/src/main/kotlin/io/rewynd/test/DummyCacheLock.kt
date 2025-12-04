package io.rewynd.test

import io.rewynd.common.cache.CacheLock
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class DummyCacheLock(
    override val timeout: Duration = 10.minutes,
    override val validUntil: Instant = Clock.System.now() + timeout,
) : CacheLock {
    private var released = false

    override fun release() {
        released = true
    }

    override fun extend(newTimeout: Duration?): CacheLock? =
        if (!released) {
            DummyCacheLock(timeout)
        } else {
            null
        }
}
