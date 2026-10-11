package com.tidemark.habits.domain

import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Convenient test builders with standard defaults matching the brief's reference calendar.
 */
fun createHabit(
    id: Long = 1L,
    name: String = "Test Habit",
    type: HabitType = HabitType.YES_NO,
    schedule: Schedule = Schedule.Daily,
    target: Int = if (type == HabitType.YES_NO) 1 else 10,
    unit: String? = if (type == HabitType.COUNTABLE) "units" else if (type == HabitType.TIMED) "min" else null,
    step: Int = 1,
    reminderTime: LocalTime? = null,
    startDate: LocalDate = LocalDate.of(2026, 10, 5),
    createdAt: Instant = Instant.parse("2026-10-05T00:00:00Z")
): Habit = Habit(
    id = id,
    name = name,
    type = type,
    schedule = schedule,
    target = target,
    unit = unit,
    step = step,
    reminderTime = reminderTime,
    startDate = startDate,
    createdAt = createdAt
)

fun createLog(
    habitId: Long = 1L,
    date: LocalDate,
    value: Int,
    completed: Boolean
): HabitLog = HabitLog(
    habitId = habitId,
    date = date,
    value = value,
    completed = completed
)
