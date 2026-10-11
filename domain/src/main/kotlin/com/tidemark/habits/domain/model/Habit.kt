package com.tidemark.habits.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Core Habit domain model representing an ongoing habit.
 *
 * @property id Unique persistent identifier (0 for unpersisted).
 * @property name User-defined name (1..40 chars).
 * @property type YES_NO, COUNTABLE, or TIMED.
 * @property schedule Frequency schedule (Daily, Weekdays, or TimesPerWeek).
 * @property target Target goal per day (1 for YES_NO; unit quantity for COUNTABLE; minutes for TIMED).
 * @property unit Measurement unit label (COUNTABLE only, e.g. "pages"; TIMED defaults to "min").
 * @property step Increment amount for quick logging (>= 1).
 * @property reminderTime Daily reminder time if enabled, null otherwise.
 * @property startDate First active date of the habit; days prior are never considered missed.
 * @property createdAt Timestamp when the habit was created.
 */
data class Habit(
    val id: Long = 0,
    val name: String,
    val type: HabitType,
    val schedule: Schedule,
    val target: Int,
    val unit: String? = null,
    val step: Int = 1,
    val reminderTime: LocalTime? = null,
    val startDate: LocalDate,
    val createdAt: Instant
)
