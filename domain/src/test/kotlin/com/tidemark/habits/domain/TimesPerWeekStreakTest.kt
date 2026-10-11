package com.tidemark.habits.domain

import com.tidemark.habits.domain.logic.StatsCalculator
import com.tidemark.habits.domain.logic.StreakCalculator
import com.tidemark.habits.domain.logic.WeekUtils
import com.tidemark.habits.domain.model.Schedule
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class TimesPerWeekStreakTest {

    // W1 = Mon 2026-10-05 to Sun 2026-10-11
    // W2 = Mon 2026-10-12 to Sun 2026-10-18
    // W3 = Mon 2026-10-19 to Sun 2026-10-25
    private val w1Mon = LocalDate.of(2026, 10, 5)
    private val w1Wed = LocalDate.of(2026, 10, 7)
    private val w1Fri = LocalDate.of(2026, 10, 9)

    private val w2Mon = LocalDate.of(2026, 10, 12)
    private val w2Wed = LocalDate.of(2026, 10, 14)

    private val w3Mon = LocalDate.of(2026, 10, 19)

    @Test
    fun `T18 - W1 has 3 completed days and today is Wed of W2 with none yields current streak 1`() {
        val habit = createHabit(schedule = Schedule.TimesPerWeek(3), startDate = w1Mon)
        val logs = listOf(
            createLog(1L, w1Mon, 1, completed = true),
            createLog(1L, w1Wed, 1, completed = true),
            createLog(1L, w1Fri, 1, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = w2Wed)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `T19 - W2 has only 2 completed days and today is in W3 yields current 0 and longest 1`() {
        val habit = createHabit(schedule = Schedule.TimesPerWeek(3), startDate = w1Mon)
        val logs = listOf(
            // W1 met (3 completions)
            createLog(1L, w1Mon, 1, completed = true),
            createLog(1L, w1Wed, 1, completed = true),
            createLog(1L, w1Fri, 1, completed = true),
            // W2 has only 2 completions
            createLog(1L, w2Mon, 1, completed = true),
            createLog(1L, w2Wed, 1, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = w3Mon)
        assertEquals(0, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `T20 - current week already 3 of 3 and W1 met yields current streak 2`() {
        val habit = createHabit(schedule = Schedule.TimesPerWeek(3), startDate = w1Mon)
        val w2Fri = LocalDate.of(2026, 10, 16)
        val logs = listOf(
            // W1 met
            createLog(1L, w1Mon, 1, completed = true),
            createLog(1L, w1Wed, 1, completed = true),
            createLog(1L, w1Fri, 1, completed = true),
            // W2 already met
            createLog(1L, w2Mon, 1, completed = true),
            createLog(1L, w2Wed, 1, completed = true),
            createLog(1L, w2Fri, 1, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = w2Fri)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun `T21 - five completions in one week count as one week`() {
        val habit = createHabit(schedule = Schedule.TimesPerWeek(3), startDate = w1Mon)
        val w1Tue = LocalDate.of(2026, 10, 6)
        val w1Thu = LocalDate.of(2026, 10, 8)
        val logs = listOf(
            createLog(1L, w1Mon, 1, completed = true),
            createLog(1L, w1Tue, 1, completed = true),
            createLog(1L, w1Wed, 1, completed = true),
            createLog(1L, w1Thu, 1, completed = true),
            createLog(1L, w1Fri, 1, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = w1Fri)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `T22 - Sunday belongs to the week it ends and Monday starts a new week`() {
        val sun = LocalDate.of(2026, 10, 11) // Ends W1
        val nextMon = LocalDate.of(2026, 10, 12) // Starts W2

        assertEquals(w1Mon, WeekUtils.startOfWeek(sun))
        assertEquals(nextMon, WeekUtils.startOfWeek(nextMon))
    }

    @Test
    fun `T23 - 2026-12-31 and 2027-01-01 belong to the same ISO week 2026-W53`() {
        val dec31 = LocalDate.of(2026, 12, 31) // Thursday
        val jan01 = LocalDate.of(2027, 1, 1)   // Friday

        val mondayDec = WeekUtils.startOfWeek(dec31)
        val mondayJan = WeekUtils.startOfWeek(jan01)

        assertEquals(mondayDec, mondayJan)
        assertEquals(LocalDate.of(2026, 12, 28), mondayDec)
    }

    @Test
    fun `T24 - first partial week is pro-rated where start Sat and N=3 requires 2 days`() {
        val sat = LocalDate.of(2026, 10, 10)
        val sun = LocalDate.of(2026, 10, 11)

        val habit = createHabit(schedule = Schedule.TimesPerWeek(3), startDate = sat)
        val logs = listOf(
            createLog(1L, sat, 1, completed = true),
            createLog(1L, sun, 1, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = sun)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `T25 - N=7 requires all seven days to be completed`() {
        val habit = createHabit(schedule = Schedule.TimesPerWeek(7), startDate = w1Mon)
        val sun = LocalDate.of(2026, 10, 11)

        // Only 6 days completed
        val sixDaysLogs = (0..5).map { offset ->
            createLog(1L, w1Mon.plusDays(offset.toLong()), 1, completed = true)
        }
        val incompleteResult = StreakCalculator.calculateStreak(habit, sixDaysLogs, today = w2Mon)
        assertEquals(0, incompleteResult.currentStreak)

        // All 7 days completed
        val sevenDaysLogs = sixDaysLogs + createLog(1L, sun, 1, completed = true)
        val completeResult = StreakCalculator.calculateStreak(habit, sevenDaysLogs, today = w2Mon)
        assertEquals(1, completeResult.currentStreak)
    }

    @Test
    fun `T26 - thisWeekProgress returns done and required`() {
        val habit = createHabit(schedule = Schedule.TimesPerWeek(3), startDate = w1Mon)
        val logs = listOf(
            createLog(1L, w1Mon, 1, completed = true),
            createLog(1L, w1Wed, 1, completed = true)
        )
        val progress = StatsCalculator.calculateThisWeekProgress(habit, logs, today = w1Wed)
        assertEquals(2, progress.done)
        assertEquals(3, progress.required)
    }
}
