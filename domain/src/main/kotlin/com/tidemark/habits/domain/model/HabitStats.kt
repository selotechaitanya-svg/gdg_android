package com.tidemark.habits.domain.model

/**
 * Aggregated statistics for an individual habit.
 *
 * @property habitId ID of the habit.
 * @property currentStreak Active streak.
 * @property longestStreak Historical best streak.
 * @property completionRate7d Rate over last 7 days (or last 4 weeks for TimesPerWeek); null if denominator is 0.
 * @property completionRate30d Rate over last 30 days (or last 12 weeks for TimesPerWeek); null if denominator is 0.
 * @property completionRateAllTime Rate since startDate; null if denominator is 0.
 * @property thisWeekProgress Current week's progress.
 * @property totalCompletions Total completed days in history.
 */
data class HabitStats(
    val habitId: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRate7d: Double?,
    val completionRate30d: Double?,
    val completionRateAllTime: Double?,
    val thisWeekProgress: WeekProgress?,
    val totalCompletions: Int
)
