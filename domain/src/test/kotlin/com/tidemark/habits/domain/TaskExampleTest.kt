package com.tidemark.habits.domain

import com.tidemark.habits.domain.logic.LogEditResult
import com.tidemark.habits.domain.logic.LogEditor
import com.tidemark.habits.domain.logic.StreakCalculator
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Normative task examples transcribed from GDG task specification (T1-T3).
 */
class TaskExampleTest {

    private val monday = LocalDate.of(2026, 10, 5)
    private val tuesday = LocalDate.of(2026, 10, 6)
    private val wednesday = LocalDate.of(2026, 10, 7)

    @Test
    fun `T1 - read habit Mon 12, Tue 20, Wed 25 yields streak 2 because Mon missed target`() {
        val read = createHabit(
            id = 1L,
            name = "Read",
            type = HabitType.COUNTABLE,
            schedule = Schedule.Daily,
            target = 20,
            unit = "pages",
            startDate = monday
        )
        val logs = listOf(
            createLog(1L, monday, 12, completed = false),
            createLog(1L, tuesday, 20, completed = true),
            createLog(1L, wednesday, 25, completed = true)
        )

        val result = StreakCalculator.calculateStreak(read, logs, today = wednesday)

        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun `T2 - gym habit Mon done, Wed done yields streak 2 because Tuesday was unscheduled`() {
        val gym = createHabit(
            id = 2L,
            name = "Gym",
            type = HabitType.YES_NO,
            schedule = Schedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
            target = 1,
            startDate = monday
        )
        val logs = listOf(
            createLog(2L, monday, 1, completed = true),
            createLog(2L, wednesday, 1, completed = true)
        )

        val result = StreakCalculator.calculateStreak(gym, logs, today = wednesday)

        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun `T3 - correcting Monday read to 20 through LogEditor updates streak to 3`() {
        val read = createHabit(
            id = 1L,
            name = "Read",
            type = HabitType.COUNTABLE,
            schedule = Schedule.Daily,
            target = 20,
            unit = "pages",
            startDate = monday
        )
        val initialLogs = listOf(
            createLog(1L, monday, 12, completed = false),
            createLog(1L, tuesday, 20, completed = true),
            createLog(1L, wednesday, 25, completed = true)
        )

        // User corrects Monday to 20 via LogEditor
        val editResult = LogEditor.evaluate(read, monday, 20, today = wednesday)
        assertTrue(editResult is LogEditResult.Upsert)
        assertEquals(true, editResult.log.completed)
        assertEquals(20, editResult.log.value)

        val updatedLogs = initialLogs.filterNot { it.date == monday } + editResult.log
        val updatedResult = StreakCalculator.calculateStreak(read, updatedLogs, today = wednesday)

        assertEquals(3, updatedResult.currentStreak)
        assertEquals(3, updatedResult.longestStreak)
    }
}
