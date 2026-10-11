package com.tidemark.habits.domain

import com.tidemark.habits.domain.logic.StreakCalculator
import com.tidemark.habits.domain.model.Schedule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals

class WeekdayStreakTest {

    private val mon = LocalDate.of(2026, 10, 5)
    private val tue = LocalDate.of(2026, 10, 6)
    private val wed = LocalDate.of(2026, 10, 7)
    private val fri = LocalDate.of(2026, 10, 9)
    private val sat = LocalDate.of(2026, 10, 10)
    private val nextMon = LocalDate.of(2026, 10, 12)

    private val monWedFri = Schedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

    @Test
    fun `T12 - unscheduled days are transparent across a weekend`() {
        // Starts Fri 2026-10-09, next scheduled is Mon 2026-10-12
        val habit = createHabit(schedule = monWedFri, startDate = fri)
        val logs = listOf(
            createLog(1L, fri, 1, completed = true),
            createLog(1L, nextMon, 1, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = nextMon)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun `T13 - missed scheduled day breaks weekday streak`() {
        val habit = createHabit(schedule = monWedFri, startDate = mon)
        val logs = listOf(
            createLog(1L, mon, 1, completed = true),
            // wed missed
            createLog(1L, fri, 1, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = fri)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `T14 - scheduled today pending maintains streak, but moving past today breaks it`() {
        val habit = createHabit(schedule = monWedFri, startDate = mon)
        val logs = listOf(
            createLog(1L, mon, 1, completed = true),
            createLog(1L, wed, 1, completed = true)
            // fri pending when today=fri
        )
        // Today is Friday (pending)
        val pendingResult = StreakCalculator.calculateStreak(habit, logs, today = fri)
        assertEquals(2, pendingResult.currentStreak)
        assertEquals(2, pendingResult.longestStreak)

        // Today is Saturday (Friday is now missed)
        val missedResult = StreakCalculator.calculateStreak(habit, logs, today = sat)
        assertEquals(0, missedResult.currentStreak)
        assertEquals(2, missedResult.longestStreak)
    }

    @Test
    fun `T15 - today unscheduled does not affect current streak`() {
        val habit = createHabit(schedule = monWedFri, startDate = mon)
        val logs = listOf(
            createLog(1L, mon, 1, completed = true)
        )
        // Tuesday is unscheduled; active streak remains 1
        val result = StreakCalculator.calculateStreak(habit, logs, today = tue)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `T16 - bonus log on an unscheduled day is ignored by streak math`() {
        val habit = createHabit(schedule = monWedFri, startDate = mon)
        val logs = listOf(
            createLog(1L, mon, 1, completed = true),
            createLog(1L, tue, 1, completed = true), // Bonus mark on unscheduled Tuesday
            createLog(1L, wed, 1, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = wed)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun `T17 - single weekday (Sundays only) computes streak correctly`() {
        val sunOnly = Schedule.Weekdays(setOf(DayOfWeek.SUNDAY))
        val sun1 = LocalDate.of(2026, 10, 11)
        val sun2 = LocalDate.of(2026, 10, 18)
        val sun3 = LocalDate.of(2026, 10, 25)

        val habit = createHabit(schedule = sunOnly, startDate = sun1)
        val logs = listOf(
            createLog(1L, sun1, 1, completed = true),
            createLog(1L, sun2, 1, completed = true),
            createLog(1L, sun3, 1, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = sun3)
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }
}
