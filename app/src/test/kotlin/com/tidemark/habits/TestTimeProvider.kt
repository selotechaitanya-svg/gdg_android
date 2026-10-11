package com.tidemark.habits

import com.tidemark.habits.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TestTimeProvider(
    private var currentInstant: Instant = Instant.parse("2026-10-12T10:00:00Z"),
    private val zoneId: ZoneId = ZoneId.of("UTC")
) : TimeProvider {

    override fun now(): Instant = currentInstant

    override fun zone(): ZoneId = zoneId

    override fun today(): LocalDate = currentInstant.atZone(zoneId).toLocalDate()

    fun advanceDays(days: Long) {
        currentInstant = currentInstant.plusSeconds(days * 86400L)
    }

    fun advanceSeconds(seconds: Long) {
        currentInstant = currentInstant.plusSeconds(seconds)
    }

    fun setDate(date: LocalDate) {
        currentInstant = date.atStartOfDay(zoneId).toInstant()
    }
}
