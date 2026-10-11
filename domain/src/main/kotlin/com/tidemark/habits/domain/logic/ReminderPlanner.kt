package com.tidemark.habits.domain.logic

import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.Schedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure logic for planning reminder triggers and determining notification delivery.
 */
object ReminderPlanner {

    /**
     * Computes the next alarm trigger instant strictly after [after] in [zone].
     * Returns null if reminders are disabled ([Habit.reminderTime] is null).
     */
    fun nextTrigger(
        habit: Habit,
        after: Instant,
        zone: ZoneId
    ): Instant? {
        val reminderTime = habit.reminderTime ?: return null

        val currentZdt = after.atZone(zone)
        val today = currentZdt.toLocalDate()
        val start = if (today.isBefore(habit.startDate)) habit.startDate else today

        // Search candidate dates for the next scheduled occurrence (up to 14 days)
        var date = start
        for (i in 0..14) {
            val isScheduled = when (val schedule = habit.schedule) {
                is Schedule.Daily -> true
                is Schedule.Weekdays -> date.dayOfWeek in schedule.days
                is Schedule.TimesPerWeek -> true
            }

            if (isScheduled) {
                val candidateZdt = ZonedDateTime.of(date, reminderTime, zone)
                val candidateInstant = candidateZdt.toInstant()
                if (candidateInstant.isAfter(after)) {
                    return candidateInstant
                }
            }
            date = date.plusDays(1)
        }

        return null
    }

    /**
     * Determines whether a reminder notification should be posted for [habit] on [date].
     *
     * Rules:
     * - False if reminders are disabled.
     * - False if date is before [Habit.startDate].
     * - False if today is already completed.
     * - For TimesPerWeek: False if the week's goal is already MET.
     * - For Weekdays: False if date is not scheduled.
     * - True if scheduled and incomplete (even if partial progress has been made).
     */
    fun shouldNotify(
        habit: Habit,
        logs: List<HabitLog>,
        date: LocalDate
    ): Boolean {
        if (habit.reminderTime == null) return false
        if (date.isBefore(habit.startDate)) return false

        val logsByDate = logs.associateBy { it.date }
        val isTodayComplete = logsByDate[date]?.completed == true
        if (isTodayComplete) return false

        return when (val schedule = habit.schedule) {
            is Schedule.Daily -> true
            is Schedule.Weekdays -> {
                ScheduleEvaluator.isScheduled(schedule, date)
            }
            is Schedule.TimesPerWeek -> {
                val weekStart = WeekUtils.startOfWeek(date)
                val weekEnd = WeekUtils.endOfWeek(date)
                val doneInWeek = logs.filter {
                    it.completed &&
                            !it.date.isBefore(habit.startDate) &&
                            !it.date.isBefore(weekStart) &&
                            !it.date.isAfter(weekEnd)
                }.map { it.date }.distinct().size

                val required = if (weekStart == WeekUtils.startOfWeek(habit.startDate)) {
                    minOf(schedule.times, WeekUtils.daysRemainingInWeek(habit.startDate))
                } else {
                    schedule.times
                }

                // If week is already met, do not notify
                doneInWeek < required
            }
        }
    }
}
