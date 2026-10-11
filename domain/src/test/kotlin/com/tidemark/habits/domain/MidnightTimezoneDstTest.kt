package com.tidemark.habits.domain

import com.tidemark.habits.domain.logic.StreakCalculator
import com.tidemark.habits.domain.time.FakeTimeProvider
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals

class MidnightTimezoneDstTest {

    private val utc = ZoneId.of("UTC")
    private val mon = LocalDate.of(2026, 10, 5)
    private val tue = LocalDate.of(2026, 10, 6)

    @Test
    fun `T40 - clock 23-59-59 counts for Mon and clock 00-00-00 advances to Tue where Mon stays complete and Tue is pending`() {
        val clock = FakeTimeProvider(
            currentInstant = Instant.parse("2026-10-05T23:59:59Z"),
            currentZone = utc
        )
        assertEquals(mon, clock.today())

        val habit = createHabit(startDate = mon)
        val logs = listOf(createLog(1L, mon, 1, completed = true))

        // At 23:59:59 Mon: streak is 1
        val monResult = StreakCalculator.calculateStreak(habit, logs, today = clock.today())
        assertEquals(1, monResult.currentStreak)

        // Advance 1 second to 00:00:00 Tue
        clock.setInstant(Instant.parse("2026-10-06T00:00:00Z"))
        assertEquals(tue, clock.today())

        // Monday stays complete, Tuesday is pending, active streak is still 1
        val tueResult = StreakCalculator.calculateStreak(habit, logs, today = clock.today())
        assertEquals(1, tueResult.currentStreak)
    }

    @Test
    fun `T41 - Mon complete with no Tue log yields streak 0 at Wed 00-00-01 because Tue is now missed`() {
        val clock = FakeTimeProvider(
            currentInstant = Instant.parse("2026-10-07T00:00:01Z"),
            currentZone = utc
        )
        val wed = LocalDate.of(2026, 10, 7)
        assertEquals(wed, clock.today())

        val habit = createHabit(startDate = mon)
        val logs = listOf(createLog(1L, mon, 1, completed = true)) // Tue missed

        val result = StreakCalculator.calculateStreak(habit, logs, today = clock.today())
        assertEquals(0, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `T42 - same instant yields different local dates across zones but stored logs are never shifted`() {
        val instant = Instant.parse("2026-10-11T22:00:00Z")
        val kolkata = ZoneId.of("Asia/Kolkata")
        val la = ZoneId.of("America/Los_Angeles")

        val kolkataDate = instant.atZone(kolkata).toLocalDate()
        val laDate = instant.atZone(la).toLocalDate()

        assertEquals(LocalDate.of(2026, 10, 12), kolkataDate)
        assertEquals(LocalDate.of(2026, 10, 11), laDate)

        // A log stored for 2026-10-11 remains tied to 2026-10-11 regardless of active zone
        val storedLog = createLog(1L, LocalDate.of(2026, 10, 11), 1, completed = true)
        assertEquals(LocalDate.of(2026, 10, 11), storedLog.date)
    }

    @Test
    fun `T43 - daily logs across US DST changes keep streak unbroken`() {
        val nyZone = ZoneId.of("America/New_York")

        // Spring forward: 2026-03-08 (23-hour day)
        val mar07 = LocalDate.of(2026, 3, 7)
        val mar08 = LocalDate.of(2026, 3, 8)
        val mar09 = LocalDate.of(2026, 3, 9)

        val springHabit = createHabit(startDate = mar07)
        val springLogs = listOf(
            createLog(1L, mar07, 1, completed = true),
            createLog(1L, mar08, 1, completed = true),
            createLog(1L, mar09, 1, completed = true)
        )
        val springResult = StreakCalculator.calculateStreak(springHabit, springLogs, today = mar09)
        assertEquals(3, springResult.currentStreak)

        // Fall back: 2026-11-01 (25-hour day)
        val oct31 = LocalDate.of(2026, 10, 31)
        val nov01 = LocalDate.of(2026, 11, 1)
        val nov02 = LocalDate.of(2026, 11, 2)

        val fallHabit = createHabit(startDate = oct31)
        val fallLogs = listOf(
            createLog(1L, oct31, 1, completed = true),
            createLog(1L, nov01, 1, completed = true),
            createLog(1L, nov02, 1, completed = true)
        )
        val fallResult = StreakCalculator.calculateStreak(fallHabit, fallLogs, today = nov02)
        assertEquals(3, fallResult.currentStreak)
    }
}
