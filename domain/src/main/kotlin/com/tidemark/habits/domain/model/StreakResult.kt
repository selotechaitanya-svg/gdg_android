package com.tidemark.habits.domain.model

/**
 * Result of a streak computation.
 *
 * @property currentStreak Active streak run (in days for Daily/Weekdays, weeks for TimesPerWeek).
 * @property longestStreak Longest streak run ever achieved. Guaranteed longestStreak >= currentStreak.
 */
data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int
)
