package com.tidemark.habits.domain

import com.tidemark.habits.domain.logic.OverviewCalculator
import com.tidemark.habits.domain.logic.StatsCalculator
import com.tidemark.habits.domain.model.DayState
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StatisticsTest {

    private val mon = LocalDate.of(2026, 10, 5)
    private val tue = LocalDate.of(2026, 10, 6)
    private val wed = LocalDate.of(2026, 10, 7)
    private val fri = LocalDate.of(2026, 10, 9)

    @Test
    fun `T27 - Daily 7-day rate where 5 of last 7 scheduled days complete and today complete yields approx 71-4 percent`() {
        val habit = createHabit(startDate = mon)
        val sun = mon.plusDays(6) // Sunday 2026-10-11
        val exactLogs = listOf(
            createLog(1L, mon, 1, completed = true),
            createLog(1L, mon.plusDays(1), 1, completed = true),
            createLog(1L, mon.plusDays(2), 1, completed = true),
            createLog(1L, mon.plusDays(3), 1, completed = true),
            createLog(1L, sun, 1, completed = true)
        )
        val rate = StatsCalculator.calculateDayRate(habit, exactLogs, windowDays = 7, today = sun)
        assertNotNull(rate)
        assertEquals(71.4, rate, 0.1) // 5/7 ≈ 71.428%
    }

    @Test
    fun `T28 - pending today is excluded from the denominator in completion rate`() {
        val habit = createHabit(startDate = mon)
        val logs = listOf(
            createLog(1L, mon, 1, completed = true)
            // Tue missing, Wed not logged
        )
        val pendingRate = StatsCalculator.calculateDayRate(habit, logs, windowDays = 7, today = wed)
        assertNotNull(pendingRate)
        // Evaluated days: Mon (complete) and Tue (missed) = 1/2 = 50.0%
        assertEquals(50.0, pendingRate, 0.01)

        // After completing Wednesday: Mon, Tue, Wed = 2/3 = 66.67%
        val completedLogs = logs + createLog(1L, wed, 1, completed = true)
        val completedRate = StatsCalculator.calculateDayRate(habit, completedLogs, windowDays = 7, today = wed)
        assertNotNull(completedRate)
        assertEquals(66.67, completedRate, 0.01)
    }

    @Test
    fun `T29 - weekdays rate counts only scheduled days`() {
        val monWedFri = Schedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        val habit = createHabit(schedule = monWedFri, startDate = mon)

        val logs = listOf(
            createLog(1L, mon, 1, completed = true)
            // Wed missed, Fri pending
        )
        val rate = StatsCalculator.calculateDayRate(habit, logs, windowDays = 7, today = fri)
        assertNotNull(rate)
        // Only Mon (complete) and Wed (missed) are evaluated: 1/2 = 50.0%
        assertEquals(50.0, rate, 0.01)
    }

    @Test
    fun `T30 - rate is null when the window has no scheduled days and all-time rate is clipped to startDate`() {
        val sunOnly = Schedule.Weekdays(setOf(DayOfWeek.SUNDAY))
        val habit = createHabit(schedule = sunOnly, startDate = tue)

        // Window Tue-Thu contains no Sundays -> 0 scheduled days
        val rate = StatsCalculator.calculateDayRate(habit, emptyList(), windowDays = 3, today = LocalDate.of(2026, 10, 8))
        assertNull(rate)

        // All-time rate starts at startDate and does not divide by zero before startDate
        val habitFuture = createHabit(startDate = LocalDate.of(2026, 10, 15))
        val rateBeforeStart = StatsCalculator.calculateDayRate(habitFuture, emptyList(), windowDays = null, today = mon)
        assertNull(rateBeforeStart)
    }

    @Test
    fun `T31 - partial progress 12 of 20 yields DayState PARTIAL and is not complete`() {
        val habit = createHabit(type = HabitType.COUNTABLE, target = 20, unit = "pages", startDate = mon)
        val logs = listOf(
            createLog(1L, mon, 12, completed = false)
        )
        val days = StatsCalculator.calculateMonthDays(habit, logs, YearMonth.of(2026, 10), today = wed)
        val monInfo = days.first { it.date == mon }
        assertEquals(DayState.PARTIAL, monInfo.state)
        assertEquals(12, monInfo.value)
    }

    @Test
    fun `T32 - N-per-week rate evaluates finished weeks and excludes an unmet in-progress week`() {
        // 4 evaluated weeks, 3 MET -> 75%
        val habit = createHabit(schedule = Schedule.TimesPerWeek(3), startDate = LocalDate.of(2026, 9, 7))

        // Week 1 (09-07..09-13): MET
        // Week 2 (09-14..09-20): MET
        // Week 3 (09-21..09-27): MET
        // Week 4 (09-28..10-04): UNMET (0 completions)
        // Week 5 (10-05..10-11): In-progress unmet today=Wed with 1 completion
        val logs = mutableListOf<com.tidemark.habits.domain.model.HabitLog>()
        for (w in 0..2) {
            val monday = LocalDate.of(2026, 9, 7).plusWeeks(w.toLong())
            logs.add(createLog(1L, monday, 1, completed = true))
            logs.add(createLog(1L, monday.plusDays(1), 1, completed = true))
            logs.add(createLog(1L, monday.plusDays(2), 1, completed = true))
        }
        // Current week (week 5) has only 1 completion (not yet MET)
        logs.add(createLog(1L, mon, 1, completed = true))

        val rate = StatsCalculator.calculateWeekRate(habit, 3, logs, windowWeeks = 4, today = wed)
        assertNotNull(rate)
        // 4 finished weeks evaluated (3 met, 1 unmet) = 3/4 = 75.0%
        assertEquals(75.0, rate, 0.01)
    }

    @Test
    fun `T33 - overview over two habits computes combined rate and best streak`() {
        val h1 = createHabit(id = 1L, name = "Habit 1", startDate = mon)
        val h2 = createHabit(id = 2L, name = "Habit 2", startDate = mon)

        val logs = listOf(
            createLog(1L, mon, 1, completed = true),
            createLog(1L, tue, 1, completed = true),
            createLog(2L, mon, 1, completed = true)
            // h2 tue missed
        )
        // With today=wed, Mon and Tue are past finished days for both habits.
        // Wed is pending (unlogged) for both and excluded from rate denominator.
        val overview = OverviewCalculator.calculateOverview(listOf(h1, h2), logs, today = wed)
        // Total scheduled = 4 (h1 Mon+Tue, h2 Mon+Tue)
        // Total complete = 3 (h1 Mon+Tue, h2 Mon)
        // Rate = 3/4 = 75.0%
        assertNotNull(overview.rate7d)
        assertEquals(75.0, overview.rate7d, 0.01)
        assertEquals("Habit 1", overview.bestStreakHabitName)
        assertEquals(2, overview.bestStreakCount)
    }

    @Test
    fun `T34 - month calendar DayStates classification`() {
        val habit = createHabit(
            schedule = Schedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
            startDate = mon
        )
        val logs = listOf(
            createLog(1L, mon, 1, completed = true),
            createLog(1L, tue, 1, completed = true) // Bonus on unscheduled Tuesday
            // Wed missed, Thu unscheduled, Fri pending
        )
        val monthDays = StatsCalculator.calculateMonthDays(habit, logs, YearMonth.of(2026, 10), today = fri)

        // Before start
        val oct4 = monthDays.first { it.date == LocalDate.of(2026, 10, 4) }
        assertEquals(DayState.BEFORE_START, oct4.state)

        // Monday complete
        val oct5 = monthDays.first { it.date == mon }
        assertEquals(DayState.COMPLETE, oct5.state)

        // Tuesday unscheduled with bonus
        val oct6 = monthDays.first { it.date == tue }
        assertEquals(DayState.UNSCHEDULED, oct6.state)
        assertEquals(true, oct6.isBonus)

        // Wednesday missed
        val oct7 = monthDays.first { it.date == wed }
        assertEquals(DayState.MISSED, oct7.state)

        // Friday pending (today)
        val oct9 = monthDays.first { it.date == fri }
        assertEquals(DayState.PENDING, oct9.state)

        // Future day
        val oct10 = monthDays.first { it.date == LocalDate.of(2026, 10, 10) }
        assertEquals(DayState.FUTURE, oct10.state)
    }
}
