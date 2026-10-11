package com.tidemark.habits.domain

import com.tidemark.habits.domain.logic.ProgressEvaluator
import com.tidemark.habits.domain.logic.StreakCalculator
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyStreakTest {

    private val mon = LocalDate.of(2026, 10, 5)
    private val tue = LocalDate.of(2026, 10, 6)
    private val wed = LocalDate.of(2026, 10, 7)
    private val thu = LocalDate.of(2026, 10, 8)
    private val fri = LocalDate.of(2026, 10, 9)
    private val sat = LocalDate.of(2026, 10, 10)

    @Test
    fun `T4 - missed day breaks daily streak`() {
        val habit = createHabit(startDate = mon)
        val logs = listOf(
            createLog(1L, mon, 1, completed = true),
            // tue missing
            createLog(1L, wed, 1, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = wed)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `T5 - today pending never breaks streak`() {
        val habit = createHabit(startDate = mon)
        val initialLogs = listOf(
            createLog(1L, mon, 1, completed = true),
            createLog(1L, tue, 1, completed = true)
            // wed pending (today)
        )
        val pendingResult = StreakCalculator.calculateStreak(habit, initialLogs, today = wed)
        assertEquals(2, pendingResult.currentStreak)
        assertEquals(2, pendingResult.longestStreak)

        // After logging Wednesday
        val completedLogs = initialLogs + createLog(1L, wed, 1, completed = true)
        val afterResult = StreakCalculator.calculateStreak(habit, completedLogs, today = wed)
        assertEquals(3, afterResult.currentStreak)
        assertEquals(3, afterResult.longestStreak)
    }

    @Test
    fun `T6 - yesterday missed and today pending yields current streak 0`() {
        val habit = createHabit(startDate = mon)
        val logs = listOf(
            createLog(1L, mon, 1, completed = true)
            // tue missed, wed pending
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = wed)
        assertEquals(0, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `T7 - longest streak survives a break`() {
        val habit = createHabit(startDate = mon)
        val logs = listOf(
            createLog(1L, mon, 1, completed = true),
            createLog(1L, tue, 1, completed = true),
            createLog(1L, wed, 1, completed = true),
            createLog(1L, thu, 1, completed = true),
            // fri missed
            createLog(1L, sat, 1, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = sat)
        assertEquals(1, result.currentStreak)
        assertEquals(4, result.longestStreak)
    }

    @Test
    fun `T8 - partial progress not complete, exact target complete, over-target complete`() {
        val habit = createHabit(type = HabitType.COUNTABLE, target = 20, unit = "pages", startDate = mon)
        assertFalse(ProgressEvaluator.isComplete(habit, 19))
        assertTrue(ProgressEvaluator.isComplete(habit, 20))
        assertTrue(ProgressEvaluator.isComplete(habit, 45))

        val logs = listOf(
            createLog(1L, mon, 45, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = mon)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `T9 - value 0 or cleared log represents a missing day`() {
        val habit = createHabit(startDate = mon)
        // Log with value 0 / completed=false acts as missed
        val logs = listOf(
            createLog(1L, mon, 1, completed = true),
            createLog(1L, tue, 0, completed = false),
            createLog(1L, wed, 1, completed = true)
        )
        val result = StreakCalculator.calculateStreak(habit, logs, today = wed)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `T10 - habit created today with no logs yields 0-0 and days before startDate are never missed`() {
        // Created today with no logs
        val habitToday = createHabit(startDate = wed)
        val resultEmpty = StreakCalculator.calculateStreak(habitToday, emptyList(), today = wed)
        assertEquals(0, resultEmpty.currentStreak)
        assertEquals(0, resultEmpty.longestStreak)

        // Created Wednesday, Wednesday logged, earlier days (Mon/Tue) not missed
        val logs = listOf(createLog(1L, wed, 1, completed = true))
        val resultStartWed = StreakCalculator.calculateStreak(habitToday, logs, today = wed)
        assertEquals(1, resultStartWed.currentStreak)
        assertEquals(1, resultStartWed.longestStreak)
    }

    @Test
    fun `T11 - streak continues across month, year, and leap-day boundaries`() {
        // Year boundary: 2027-12-31 -> 2028-01-01
        val dec31 = LocalDate.of(2027, 12, 31)
        val jan01 = LocalDate.of(2028, 1, 1)
        val habitYear = createHabit(startDate = dec31)
        val yearLogs = listOf(
            createLog(1L, dec31, 1, completed = true),
            createLog(1L, jan01, 1, completed = true)
        )
        val yearResult = StreakCalculator.calculateStreak(habitYear, yearLogs, today = jan01)
        assertEquals(2, yearResult.currentStreak)
        assertEquals(2, yearResult.longestStreak)

        // Leap day boundary: 2028-02-28 -> 2028-02-29 -> 2028-03-01
        val feb28 = LocalDate.of(2028, 2, 28)
        val feb29 = LocalDate.of(2028, 2, 29)
        val mar01 = LocalDate.of(2028, 3, 1)
        val habitLeap = createHabit(startDate = feb28)
        val leapLogs = listOf(
            createLog(1L, feb28, 1, completed = true),
            createLog(1L, feb29, 1, completed = true),
            createLog(1L, mar01, 1, completed = true)
        )
        val leapResult = StreakCalculator.calculateStreak(habitLeap, leapLogs, today = mar01)
        assertEquals(3, leapResult.currentStreak)
        assertEquals(3, leapResult.longestStreak)
    }
}
