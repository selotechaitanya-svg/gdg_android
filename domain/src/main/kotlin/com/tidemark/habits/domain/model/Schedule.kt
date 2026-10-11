package com.tidemark.habits.domain.model

import java.time.DayOfWeek

/**
 * Frequency schedule for habit execution.
 */
sealed interface Schedule {
    /**
     * Habit is scheduled every day.
     */
    data object Daily : Schedule

    /**
     * Habit is scheduled on specific days of the week.
     * @param days Set of scheduled days.
     */
    data class Weekdays(val days: Set<DayOfWeek>) : Schedule

    /**
     * Habit is scheduled to be performed N times per ISO week (Monday-Sunday).
     * @param times Number of times per week (1..7).
     */
    data class TimesPerWeek(val times: Int) : Schedule
}
