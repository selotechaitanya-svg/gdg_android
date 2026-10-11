package com.tidemark.habits.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Test implementation of [TimeProvider] with deterministically settable time and zone.
 */
class FakeTimeProvider(
    private var currentInstant: Instant = Instant.parse("2026-10-05T12:00:00Z"),
    private var currentZone: ZoneId = ZoneId.of("UTC")
) : TimeProvider {

    override fun now(): Instant = currentInstant

    override fun zone(): ZoneId = currentZone

    override fun today(): LocalDate = currentInstant.atZone(currentZone).toLocalDate()

    fun setInstant(instant: Instant) {
        currentInstant = instant
    }

    fun setZone(zone: ZoneId) {
        currentZone = zone
    }

    fun setDate(date: LocalDate) {
        currentInstant = date.atStartOfDay(currentZone).toInstant()
    }
}
