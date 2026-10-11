package com.tidemark.habits.domain.logic

import com.tidemark.habits.domain.model.Schedule
import java.time.LocalDate

/**
 * Pure schedule evaluation logic.
 */
object ScheduleEvaluator {
    /**
     * Returns true if [date] is an active scheduled day for [schedule].
     *
     * - [Schedule.Daily]: Every calendar day is scheduled.
     * - [Schedule.Weekdays]: Only days contained in [Schedule.Weekdays.days] are scheduled.
     * - [Schedule.TimesPerWeek]: Every calendar day can be logged towards the weekly quota.
     */
    fun isScheduled(schedule: Schedule, date: LocalDate): Boolean {
        return when (schedule) {
            is Schedule.Daily -> true
            is Schedule.Weekdays -> date.dayOfWeek in schedule.days
            is Schedule.TimesPerWeek -> true
        }
    }
}
