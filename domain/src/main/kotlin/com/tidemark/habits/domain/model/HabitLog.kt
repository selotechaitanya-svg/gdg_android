package com.tidemark.habits.domain.model

import java.time.LocalDate

/**
 * Log entry for a single habit on a given calendar date.
 *
 * @property habitId ID of the associated habit.
 * @property date Local date of the log.
 * @property value Recorded numeric progress (0 or 1 for YES_NO, units/minutes for others).
 * @property completed True if progress meets or exceeds the target.
 */
data class HabitLog(
    val habitId: Long,
    val date: LocalDate,
    val value: Int,
    val completed: Boolean
)
