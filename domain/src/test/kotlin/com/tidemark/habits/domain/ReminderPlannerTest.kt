package com.tidemark.habits.domain

import com.tidemark.habits.domain.logic.ReminderPlanner
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderPlannerTest {

    private val utc = ZoneId.of("UTC")
    private val mon = LocalDate.of(2026, 10, 5)
    private val fri = LocalDate.of(2026, 10, 9)

    @Test
    fun `T44 - Daily 21-00 triggers today when before 21-00 and triggers tomorrow when at or past 21-00`() {
        val habit = createHabit(
            reminderTime = LocalTime.of(21, 0),
            startDate = mon
        )
        // Now 20:59:59 on Mon 2026-10-05
        val before21 = Instant.parse("2026-10-05T20:59:59Z")
        val todayTrigger = ReminderPlanner.nextTrigger(habit, after = before21, zone = utc)
        assertNotNull(todayTrigger)
        assertEquals(Instant.parse("2026-10-05T21:00:00Z"), todayTrigger)

        // Now exactly 21:00:00 on Mon 2026-10-05 -> tomorrow 21:00
        val exactly21 = Instant.parse("2026-10-05T21:00:00Z")
        val tomorrowTrigger = ReminderPlanner.nextTrigger(habit, after = exactly21, zone = utc)
        assertNotNull(tomorrowTrigger)
        assertEquals(Instant.parse("2026-10-06T21:00:00Z"), tomorrowTrigger)
    }

    @Test
    fun `T45 - Mon-Wed-Fri 07-30 triggers Monday when called at Friday 08-00`() {
        val monWedFri = Schedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        val habit = createHabit(
            schedule = monWedFri,
            reminderTime = LocalTime.of(7, 30),
            startDate = mon
        )
        // Now Fri 2026-10-09 at 08:00:00 (past Friday's 07:30 reminder)
        val friPast = Instant.parse("2026-10-09T08:00:00Z")
        val nextTrigger = ReminderPlanner.nextTrigger(habit, after = friPast, zone = utc)
        assertNotNull(nextTrigger)
        // Next scheduled day is Monday 2026-10-12 at 07:30
        assertEquals(Instant.parse("2026-10-12T07:30:00Z"), nextTrigger)
    }

    @Test
    fun `T46 - shouldNotify is false when already complete or week already MET and true for partial progress`() {
        val habit = createHabit(
            type = HabitType.COUNTABLE,
            target = 20,
            unit = "pages",
            reminderTime = LocalTime.of(20, 0),
            startDate = mon
        )
        // When completed: false
        val completeLogs = listOf(createLog(1L, mon, 20, completed = true))
        assertFalse(ReminderPlanner.shouldNotify(habit, completeLogs, date = mon))

        // When partial progress: true
        val partialLogs = listOf(createLog(1L, mon, 12, completed = false))
        assertTrue(ReminderPlanner.shouldNotify(habit, partialLogs, date = mon))

        // TimesPerWeek: false when week is already MET
        val weeklyHabit = createHabit(
            schedule = Schedule.TimesPerWeek(2),
            reminderTime = LocalTime.of(20, 0),
            startDate = mon
        )
        val wed = LocalDate.of(2026, 10, 7)
        val thu = LocalDate.of(2026, 10, 8)
        val metWeeklyLogs = listOf(
            createLog(1L, mon, 1, completed = true),
            createLog(1L, wed, 1, completed = true)
        )
        // On Thursday, the week's quota of 2 is already met -> false
        assertFalse(ReminderPlanner.shouldNotify(weeklyHabit, metWeeklyLogs, date = thu))
    }

    @Test
    fun `T47 - no reminder returns null and nonexistent local time shifts forward correctly`() {
        // No reminder -> null
        val noReminderHabit = createHabit(reminderTime = null, startDate = mon)
        assertNull(ReminderPlanner.nextTrigger(noReminderHabit, Instant.parse("2026-10-05T12:00:00Z"), utc))

        // Nonexistent time 02:30 on 2026-03-08 in America/New_York (spring forward gap)
        val nyZone = ZoneId.of("America/New_York")
        val dstDate = LocalDate.of(2026, 3, 8)
        val gapTime = LocalTime.of(2, 30)

        val dstHabit = createHabit(
            reminderTime = gapTime,
            startDate = dstDate
        )
        // After 01:00 on that day
        val beforeGapInstant = ZonedDateTime.of(dstDate, LocalTime.of(1, 0), nyZone).toInstant()
        val triggerInstant = ReminderPlanner.nextTrigger(dstHabit, after = beforeGapInstant, zone = nyZone)
        assertNotNull(triggerInstant)

        val triggerZdt = triggerInstant.atZone(nyZone)
        // Java Time shifts the nonexistent 02:30 forward by 1 hour to 03:30!
        assertEquals(LocalTime.of(3, 30), triggerZdt.toLocalTime())
    }
}
