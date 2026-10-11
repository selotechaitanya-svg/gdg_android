package com.tidemark.habits.domain.time

import java.time.Instant
import java.time.ZoneId

/**
 * Production implementation of [TimeProvider] delegating to system clock and default zone.
 */
class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
