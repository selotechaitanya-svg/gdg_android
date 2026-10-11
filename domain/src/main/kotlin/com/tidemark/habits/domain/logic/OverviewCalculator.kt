package com.tidemark.habits.domain.logic

import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.OverviewStats
import java.time.LocalDate

/**
 * Pure calculator for application-wide statistics across all active habits.
 */
object OverviewCalculator {

    /**
     * Computes aggregate overview metrics across all habits.
     */
    fun calculateOverview(
        habits: List<Habit>,
        logs: List<HabitLog>,
        today: LocalDate
    ): OverviewStats {
        if (habits.isEmpty()) {
            return OverviewStats(
                completionsToday = 0,
                rate7d = null,
                rate30d = null,
                bestStreakHabitName = null,
                bestStreakCount = 0
            )
        }

        val logsByHabit = logs.groupBy { it.habitId }

        val completionsToday = logs.count { it.date == today && it.completed }

        var bestStreak = 0
        var bestStreakName: String? = null
        for (habit in habits) {
            val habitLogs = logsByHabit[habit.id] ?: emptyList()
            val streak = StreakCalculator.calculateStreak(habit, habitLogs, today)
            if (streak.currentStreak > bestStreak || bestStreakName == null) {
                bestStreak = streak.currentStreak
                bestStreakName = habit.name
            }
        }

        val rate7d = calculateAggregateRate(habits, logsByHabit, windowDays = 7, today = today)
        val rate30d = calculateAggregateRate(habits, logsByHabit, windowDays = 30, today = today)

        return OverviewStats(
            completionsToday = completionsToday,
            rate7d = rate7d,
            rate30d = rate30d,
            bestStreakHabitName = bestStreakName,
            bestStreakCount = bestStreak
        )
    }

    private fun calculateAggregateRate(
        habits: List<Habit>,
        logsByHabit: Map<Long, List<HabitLog>>,
        windowDays: Int,
        today: LocalDate
    ): Double? {
        var totalScheduled = 0
        var totalCompleted = 0

        for (habit in habits) {
            if (habit.startDate.isAfter(today)) continue

            val windowStart = today.minusDays((windowDays - 1).toLong()).let {
                if (it.isBefore(habit.startDate)) habit.startDate else it
            }
            if (windowStart.isAfter(today)) continue

            val habitLogs = logsByHabit[habit.id] ?: emptyList()
            val logsByDate = habitLogs.associateBy { it.date }

            var curr = windowStart
            while (!curr.isAfter(today)) {
                if (ScheduleEvaluator.isScheduled(habit.schedule, curr)) {
                    val isComplete = logsByDate[curr]?.completed == true
                    if (isComplete) {
                        totalScheduled++
                        totalCompleted++
                    } else if (curr == today) {
                        // Pending today excluded from denominator
                    } else {
                        totalScheduled++
                    }
                }
                curr = curr.plusDays(1)
            }
        }

        if (totalScheduled == 0) return null
        return (totalCompleted.toDouble() / totalScheduled.toDouble()) * 100.0
    }
}
