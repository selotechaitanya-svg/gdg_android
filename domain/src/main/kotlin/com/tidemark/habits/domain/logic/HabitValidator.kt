package com.tidemark.habits.domain.logic

import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import java.time.LocalDate

/**
 * Typed validation errors returned by [HabitValidator].
 */
sealed interface HabitValidationError {
    /** Name is blank. */
    data object BlankName : HabitValidationError

    /** Name exceeds maximum character limit. */
    data class NameTooLong(val length: Int, val max: Int = 40) : HabitValidationError

    /** Target is outside valid bounds [1, 100000]. */
    data class InvalidTarget(val target: Int, val min: Int = 1, val max: Int = 100000) : HabitValidationError

    /** Timed target is outside valid minutes bounds [1, 1440]. */
    data class InvalidTimedTarget(val target: Int, val min: Int = 1, val max: Int = 1440) : HabitValidationError

    /** Countable habits require a unit label. */
    data object MissingUnit : HabitValidationError

    /** Unit label exceeds 12 characters. */
    data class UnitTooLong(val length: Int, val max: Int = 12) : HabitValidationError

    /** Weekday schedule must select at least one day. */
    data object EmptyWeekdays : HabitValidationError

    /** TimesPerWeek must be between 1 and 7. */
    data class InvalidTimesPerWeek(val times: Int) : HabitValidationError

    /** Step increment must be >= 1. */
    data class InvalidStep(val step: Int) : HabitValidationError

    /** Start date cannot be in the future. */
    data class FutureStartDate(val startDate: LocalDate, val today: LocalDate) : HabitValidationError
}

/**
 * Pure validation engine for habit definitions.
 */
object HabitValidator {
    /**
     * Validates [habit] against business rules using [today] as the upper bound for startDate.
     * Returns a list of all validation errors found (empty list if valid).
     */
    fun validate(habit: Habit, today: LocalDate): List<HabitValidationError> {
        val errors = mutableListOf<HabitValidationError>()

        val trimmedName = habit.name.trim()
        if (trimmedName.isEmpty()) {
            errors.add(HabitValidationError.BlankName)
        } else if (trimmedName.length > 40) {
            errors.add(HabitValidationError.NameTooLong(trimmedName.length, 40))
        }

        when (habit.type) {
            HabitType.YES_NO -> {
                // Target is canonically 1
            }
            HabitType.COUNTABLE -> {
                if (habit.target < 1 || habit.target > 100000) {
                    errors.add(HabitValidationError.InvalidTarget(habit.target, 1, 100000))
                }
                val unit = habit.unit?.trim()
                if (unit.isNullOrEmpty()) {
                    errors.add(HabitValidationError.MissingUnit)
                } else if (unit.length > 12) {
                    errors.add(HabitValidationError.UnitTooLong(unit.length, 12))
                }
            }
            HabitType.TIMED -> {
                if (habit.target < 1 || habit.target > 1440) {
                    errors.add(HabitValidationError.InvalidTimedTarget(habit.target, 1, 1440))
                }
            }
        }

        when (val schedule = habit.schedule) {
            is Schedule.Daily -> Unit
            is Schedule.Weekdays -> {
                if (schedule.days.isEmpty()) {
                    errors.add(HabitValidationError.EmptyWeekdays)
                }
            }
            is Schedule.TimesPerWeek -> {
                if (schedule.times !in 1..7) {
                    errors.add(HabitValidationError.InvalidTimesPerWeek(schedule.times))
                }
            }
        }

        if (habit.step < 1) {
            errors.add(HabitValidationError.InvalidStep(habit.step))
        }

        if (habit.startDate.isAfter(today)) {
            errors.add(HabitValidationError.FutureStartDate(habit.startDate, today))
        }

        return errors
    }
}
