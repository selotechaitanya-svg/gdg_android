package com.tidemark.habits.domain.logic

import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.Schedule
import com.tidemark.habits.domain.model.StreakResult
import java.time.LocalDate

/**
 * Pure streak engine for Tidemark.
 *
 * Vocabulary & Invariants:
 * - A day is COMPLETE if a log with completed == true exists.
 * - Only dates in [habit.startDate, today] are evaluated. Days prior to startDate are never missed.
 * - For Daily / Weekdays:
 *   - Unscheduled days are transparent (neither extend nor break a streak).
 *   - Scheduled days are classified as COMPLETE, MISSED (before today, not complete), or PENDING (today, not complete).
 *   - Trailing PENDING is dropped before evaluating the current streak so an in-progress today never resets a streak.
 *   - currentStreak = trailing run of COMPLETE.
 *   - longestStreak = longest run of COMPLETE anywhere.
 * - For TimesPerWeek(N):
 *   - Week = ISO week (Monday–Sunday).
 *   - doneInWeek = count of distinct COMPLETE days in that week.
 *   - First partial week goal is pro-rated: min(N, days from startDate to week Sunday).
 *   - Classified as MET (doneInWeek >= required), UNMET (past week, < required), or PENDING (current week, not yet met).
 *   - Trailing PENDING is dropped. currentStreak = trailing run of MET.
 */
object StreakCalculator {
    private enum class DayClassification { COMPLETE, MISSED, PENDING }
    private enum class WeekClassification { MET, UNMET, PENDING }

    /**
     * Computes the current and longest streak for [habit] given [logs] up to [today].
     */
    fun calculateStreak(
        habit: Habit,
        logs: List<HabitLog>,
        today: LocalDate
    ): StreakResult {
        if (habit.startDate.isAfter(today)) {
            return StreakResult(currentStreak = 0, longestStreak = 0)
        }

        return when (val schedule = habit.schedule) {
            is Schedule.Daily, is Schedule.Weekdays -> calculateDayStreak(habit, logs, today)
            is Schedule.TimesPerWeek -> calculateWeekStreak(habit, schedule.times, logs, today)
        }
    }

    private fun calculateDayStreak(
        habit: Habit,
        logs: List<HabitLog>,
        today: LocalDate
    ): StreakResult {
        val logsByDate = logs.associateBy { it.date }
        val classifications = mutableListOf<DayClassification>()

        var curr = habit.startDate
        while (!curr.isAfter(today)) {
            if (ScheduleEvaluator.isScheduled(habit.schedule, curr)) {
                val isComplete = logsByDate[curr]?.completed == true
                if (isComplete) {
                    classifications.add(DayClassification.COMPLETE)
                } else if (curr == today) {
                    classifications.add(DayClassification.PENDING)
                } else {
                    classifications.add(DayClassification.MISSED)
                }
            }
            curr = curr.plusDays(1)
        }

        var longestStreak = 0
        var currentRun = 0
        for (c in classifications) {
            if (c == DayClassification.COMPLETE) {
                currentRun++
                if (currentRun > longestStreak) {
                    longestStreak = currentRun
                }
            } else {
                currentRun = 0
            }
        }

        val evalList = if (classifications.isNotEmpty() && classifications.last() == DayClassification.PENDING) {
            classifications.dropLast(1)
        } else {
            classifications
        }

        var trailingStreak = 0
        for (i in evalList.indices.reversed()) {
            if (evalList[i] == DayClassification.COMPLETE) {
                trailingStreak++
            } else {
                break
            }
        }

        return StreakResult(
            currentStreak = trailingStreak,
            longestStreak = maxOf(longestStreak, trailingStreak)
        )
    }

    private fun calculateWeekStreak(
        habit: Habit,
        timesPerWeek: Int,
        logs: List<HabitLog>,
        today: LocalDate
    ): StreakResult {
        val completedDates = logs.filter { it.completed && !it.date.isBefore(habit.startDate) && !it.date.isAfter(today) }
            .map { it.date }
            .toSet()

        val weeks = WeekUtils.weeksBetween(habit.startDate, today)
        val currentWeekMonday = WeekUtils.startOfWeek(today)
        val firstWeekMonday = WeekUtils.startOfWeek(habit.startDate)

        val classifications = mutableListOf<WeekClassification>()

        for (monday in weeks) {
            val sunday = WeekUtils.endOfWeek(monday)
            val doneInWeek = completedDates.count { date ->
                !date.isBefore(monday) && !date.isAfter(sunday)
            }

            val required = if (monday == firstWeekMonday) {
                minOf(timesPerWeek, WeekUtils.daysRemainingInWeek(habit.startDate))
            } else {
                timesPerWeek
            }

            if (doneInWeek >= required) {
                classifications.add(WeekClassification.MET)
            } else if (monday == currentWeekMonday) {
                classifications.add(WeekClassification.PENDING)
            } else {
                classifications.add(WeekClassification.UNMET)
            }
        }

        var longestStreak = 0
        var currentRun = 0
        for (c in classifications) {
            if (c == WeekClassification.MET) {
                currentRun++
                if (currentRun > longestStreak) {
                    longestStreak = currentRun
                }
            } else {
                currentRun = 0
            }
        }

        val evalList = if (classifications.isNotEmpty() && classifications.last() == WeekClassification.PENDING) {
            classifications.dropLast(1)
        } else {
            classifications
        }

        var trailingStreak = 0
        for (i in evalList.indices.reversed()) {
            if (evalList[i] == WeekClassification.MET) {
                trailingStreak++
            } else {
                break
            }
        }

        return StreakResult(
            currentStreak = trailingStreak,
            longestStreak = maxOf(longestStreak, trailingStreak)
        )
    }
}
