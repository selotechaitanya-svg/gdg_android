package com.tidemark.habits.domain

import com.tidemark.habits.domain.logic.HabitTypeConverter
import com.tidemark.habits.domain.logic.HabitValidationError
import com.tidemark.habits.domain.logic.HabitValidator
import com.tidemark.habits.domain.logic.LogEditResult
import com.tidemark.habits.domain.logic.LogEditor
import com.tidemark.habits.domain.logic.LogValidationError
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ValidationAndConverterTest {

    private val today = LocalDate.of(2026, 10, 5)

    @Test
    fun `T48 - type conversion adjusts values and flags correctly`() {
        // YES_NO -> COUNTABLE (target 20)
        val yesNoCompleted = HabitLog(1L, today, 1, completed = true)
        val yesNoIncomplete = HabitLog(1L, today.minusDays(1), 0, completed = false)

        val toCountable1 = HabitTypeConverter.convertLog(yesNoCompleted, HabitType.YES_NO, HabitType.COUNTABLE, newTarget = 20)
        assertEquals(20, toCountable1.value)
        assertTrue(toCountable1.completed)

        val toCountable2 = HabitTypeConverter.convertLog(yesNoIncomplete, HabitType.YES_NO, HabitType.COUNTABLE, newTarget = 20)
        assertEquals(0, toCountable2.value)
        assertEquals(false, toCountable2.completed)

        // COUNTABLE -> YES_NO
        val countableCompleted = HabitLog(1L, today, 25, completed = true)
        val countableIncomplete = HabitLog(1L, today.minusDays(1), 10, completed = false)

        val toYesNo1 = HabitTypeConverter.convertLog(countableCompleted, HabitType.COUNTABLE, HabitType.YES_NO, newTarget = 1)
        assertEquals(1, toYesNo1.value)
        assertTrue(toYesNo1.completed)

        val toYesNo2 = HabitTypeConverter.convertLog(countableIncomplete, HabitType.COUNTABLE, HabitType.YES_NO, newTarget = 1)
        assertEquals(0, toYesNo2.value)
        assertEquals(false, toYesNo2.completed)

        // COUNTABLE <-> TIMED keeps values and flags
        val countable = HabitLog(1L, today, 15, completed = true)
        val toTimed = HabitTypeConverter.convertLog(countable, HabitType.COUNTABLE, HabitType.TIMED, newTarget = 15)
        assertEquals(15, toTimed.value)
        assertTrue(toTimed.completed)
    }

    @Test
    fun `T49 - validation rejects blank or overlong name, empty weekdays, N outside bounds, target bounds, missing unit, and future start date`() {
        // Blank name
        val blankName = createHabit(name = "   ", startDate = today)
        assertTrue(HabitValidator.validate(blankName, today).any { it is HabitValidationError.BlankName })

        // Overlong name (> 40 chars)
        val longName = createHabit(name = "A".repeat(41), startDate = today)
        assertTrue(HabitValidator.validate(longName, today).any { it is HabitValidationError.NameTooLong })

        // Invalid target (< 1)
        val targetZero = createHabit(type = HabitType.COUNTABLE, target = 0, unit = "pages", startDate = today)
        assertTrue(HabitValidator.validate(targetZero, today).any { it is HabitValidationError.InvalidTarget })

        // Timed target (> 1440)
        val timedOverMax = createHabit(type = HabitType.TIMED, target = 1441, startDate = today)
        assertTrue(HabitValidator.validate(timedOverMax, today).any { it is HabitValidationError.InvalidTimedTarget })

        // COUNTABLE without unit
        val noUnit = createHabit(type = HabitType.COUNTABLE, target = 10, unit = "", startDate = today)
        assertTrue(HabitValidator.validate(noUnit, today).any { it is HabitValidationError.MissingUnit })

        // Empty weekdays
        val emptyDays = createHabit(schedule = Schedule.Weekdays(emptySet()), startDate = today)
        assertTrue(HabitValidator.validate(emptyDays, today).any { it is HabitValidationError.EmptyWeekdays })

        // N outside 1..7
        val invalidN = createHabit(schedule = Schedule.TimesPerWeek(0), startDate = today)
        assertTrue(HabitValidator.validate(invalidN, today).any { it is HabitValidationError.InvalidTimesPerWeek })

        // Invalid step (< 1)
        val invalidStep = createHabit(step = 0, startDate = today)
        assertTrue(HabitValidator.validate(invalidStep, today).any { it is HabitValidationError.InvalidStep })

        // Future start date
        val futureStart = createHabit(startDate = today.plusDays(1))
        assertTrue(HabitValidator.validate(futureStart, today).any { it is HabitValidationError.FutureStartDate })
    }

    @Test
    fun `LogEditor rejects future dates and dates before startDate, and value 0 clears`() {
        val habit = createHabit(startDate = today)

        // Future date
        val futureResult = LogEditor.evaluate(habit, today.plusDays(1), 5, today)
        assertIs<LogEditResult.Error>(futureResult)
        assertIs<LogValidationError.FutureDate>(futureResult.error)

        // Date before start date
        val beforeResult = LogEditor.evaluate(habit, today.minusDays(1), 5, today)
        assertIs<LogEditResult.Error>(beforeResult)
        assertIs<LogValidationError.BeforeStartDate>(beforeResult.error)

        // Value 0 clears
        val clearResult = LogEditor.evaluate(habit, today, 0, today)
        assertIs<LogEditResult.Clear>(clearResult)

        // Negative value error
        val negResult = LogEditor.evaluate(habit, today, -1, today)
        assertIs<LogEditResult.Error>(negResult)
        assertIs<LogValidationError.NegativeValue>(negResult.error)
    }
}
