package com.tidemark.habits.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Abstraction for system time and time-zone operations.
 * Allows pure, deterministic testing of midnight transitions, time-zones, and DST shifts.
 */
interface TimeProvider {
    /** Current instant in time. */
    fun now(): Instant

    /** Current time zone. */
    fun zone(): ZoneId

    /** Current calendar date evaluated in [zone]. */
    fun today(): LocalDate = now().atZone(zone()).toLocalDate()
}
