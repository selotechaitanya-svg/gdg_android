package com.tidemark.habits.domain.logic

import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import java.time.LocalDate

/**
 * Validation errors when editing or creating a habit log.
 */
sealed interface LogValidationError {
    /** Target date is after today. */
    data class FutureDate(val date: LocalDate, val today: LocalDate) : LogValidationError

    /** Target date is before the habit's start date. */
    data class BeforeStartDate(val date: LocalDate, val startDate: LocalDate) : LogValidationError

    /** Negative values are rejected. */
    data class NegativeValue(val value: Int) : LogValidationError
}

/**
 * Result of evaluating a habit log edit.
 */
sealed interface LogEditResult {
    /** A valid log entry should be inserted or replaced. */
    data class Upsert(val log: HabitLog) : LogEditResult

    /** The log for this date should be cleared / deleted (value == 0). */
    data object Clear : LogEditResult

    /** The edit was rejected with a validation error. */
    data class Error(val error: LogValidationError) : LogEditResult
}

/**
 * Pure business logic for editing and validating habit logs.
 * Enforces:
 * - Rejecting dates before habit.startDate
 * - Rejecting future dates
 * - Value 0 / "Clear" triggers deletion
 * - Re-evaluating completion against current habit target
 */
object LogEditor {
    /**
     * Evaluates an attempt to set [value] on [date] for [habit].
     */
    fun evaluate(
        habit: Habit,
        date: LocalDate,
        value: Int,
        today: LocalDate
    ): LogEditResult {
        if (date.isAfter(today)) {
            return LogEditResult.Error(LogValidationError.FutureDate(date, today))
        }
        if (date.isBefore(habit.startDate)) {
            return LogEditResult.Error(LogValidationError.BeforeStartDate(date, habit.startDate))
        }
        if (value < 0) {
            return LogEditResult.Error(LogValidationError.NegativeValue(value))
        }
        if (value == 0) {
            return LogEditResult.Clear
        }

        val completed = ProgressEvaluator.isComplete(habit, value)
        return LogEditResult.Upsert(
            HabitLog(
                habitId = habit.id,
                date = date,
                value = value,
                completed = completed
            )
        )
    }
}
