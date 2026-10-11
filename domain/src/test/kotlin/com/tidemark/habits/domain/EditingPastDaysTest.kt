package com.tidemark.habits.domain

import com.tidemark.habits.domain.logic.LogEditResult
import com.tidemark.habits.domain.logic.LogEditor
import com.tidemark.habits.domain.logic.StreakCalculator
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitType
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EditingPastDaysTest {

    private val mon = LocalDate.of(2026, 10, 5)
    private val tue = LocalDate.of(2026, 10, 6)
    private val wed = LocalDate.of(2026, 10, 7)
    private val thu = LocalDate.of(2026, 10, 8)

    @Test
    fun `T35 - correcting 12 to 20 flips completed and the streak updates`() {
        val habit = createHabit(type = HabitType.COUNTABLE, target = 20, unit = "pages", startDate = mon)
        val initialLogs = listOf(
            createLog(1L, mon, 12, completed = false),
            createLog(1L, tue, 20, completed = true)
        )
        val initialStreak = StreakCalculator.calculateStreak(habit, initialLogs, today = tue)
        assertEquals(1, initialStreak.currentStreak)

        val editResult = LogEditor.evaluate(habit, mon, 20, today = tue)
        assertIs<LogEditResult.Upsert>(editResult)
        assertTrue(editResult.log.completed)

        val updatedLogs = initialLogs.filterNot { it.date == mon } + editResult.log
        val updatedStreak = StreakCalculator.calculateStreak(habit, updatedLogs, today = tue)
        assertEquals(2, updatedStreak.currentStreak)
    }

    @Test
    fun `T36 - lowering a completed past day below target breaks the streak retroactively`() {
        val habit = createHabit(type = HabitType.COUNTABLE, target = 20, unit = "pages", startDate = mon)
        val initialLogs = listOf(
            createLog(1L, mon, 20, completed = true),
            createLog(1L, tue, 20, completed = true),
            createLog(1L, wed, 20, completed = true)
        )
        val initialStreak = StreakCalculator.calculateStreak(habit, initialLogs, today = wed)
        assertEquals(3, initialStreak.currentStreak)

        // User lowers Tuesday from 20 down to 5
        val editResult = LogEditor.evaluate(habit, tue, 5, today = wed)
        assertIs<LogEditResult.Upsert>(editResult)
        assertEquals(false, editResult.log.completed)

        val updatedLogs = initialLogs.filterNot { it.date == tue } + editResult.log
        val updatedStreak = StreakCalculator.calculateStreak(habit, updatedLogs, today = wed)
        assertEquals(1, updatedStreak.currentStreak)
        assertEquals(1, updatedStreak.longestStreak)
    }

    @Test
    fun `T37 - filling a gap merges streaks`() {
        val habit = createHabit(startDate = mon)
        val gapLogs = listOf(
            createLog(1L, mon, 1, completed = true),
            // tue missing
            createLog(1L, wed, 1, completed = true),
            createLog(1L, thu, 1, completed = true)
        )
        val gapStreak = StreakCalculator.calculateStreak(habit, gapLogs, today = thu)
        assertEquals(2, gapStreak.currentStreak)
        assertEquals(2, gapStreak.longestStreak)

        // Filling Tuesday's gap
        val editResult = LogEditor.evaluate(habit, tue, 1, today = thu)
        assertIs<LogEditResult.Upsert>(editResult)

        val mergedLogs = gapLogs + editResult.log
        val mergedStreak = StreakCalculator.calculateStreak(habit, mergedLogs, today = thu)
        assertEquals(4, mergedStreak.currentStreak)
        assertEquals(4, mergedStreak.longestStreak)
    }

    @Test
    fun `T38 - value 0 clears the log and invalid dates are rejected with typed error`() {
        val habit = createHabit(startDate = mon)

        val clearResult = LogEditor.evaluate(habit, mon, 0, today = thu)
        assertIs<LogEditResult.Clear>(clearResult)

        val futureResult = LogEditor.evaluate(habit, thu.plusDays(1), 1, today = thu)
        assertIs<LogEditResult.Error>(futureResult)

        val beforeResult = LogEditor.evaluate(habit, mon.minusDays(1), 1, today = thu)
        assertIs<LogEditResult.Error>(beforeResult)
    }

    @Test
    fun `T39 - changing habit target keeps old completed flags while re-editing an old day uses new target`() {
        // Initial habit with target 10
        val habitTarget10 = createHabit(type = HabitType.COUNTABLE, target = 10, unit = "pages", startDate = mon)
        val logs = listOf(
            createLog(1L, mon, 10, completed = true)
        )
        // Stored completed flag stays true even if habit target is updated to 20
        val habitTarget20 = habitTarget10.copy(target = 20)
        val streakTarget20 = StreakCalculator.calculateStreak(habitTarget20, logs, today = mon)
        assertEquals(1, streakTarget20.currentStreak)

        // Now user re-edits Monday keeping value 10: with the new target 20, 10 is no longer complete
        val reEditResult = LogEditor.evaluate(habitTarget20, mon, 10, today = mon)
        assertIs<LogEditResult.Upsert>(reEditResult)
        assertEquals(false, reEditResult.log.completed)

        val updatedLogs = listOf(reEditResult.log)
        val updatedStreak = StreakCalculator.calculateStreak(habitTarget20, updatedLogs, today = mon)
        assertEquals(0, updatedStreak.currentStreak)
    }
}
