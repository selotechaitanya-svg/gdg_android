package com.tidemark.habits.domain.model

/**
 * Overview statistics across all active habits.
 *
 * @property completionsToday Count of habits completed today.
 * @property rate7d Aggregate completion rate over the last 7 days (sum completed / sum scheduled); null if no scheduled days.
 * @property rate30d Aggregate completion rate over the last 30 days (sum completed / sum scheduled); null if no scheduled days.
 * @property bestStreakHabitName Name of the habit holding the highest current streak.
 * @property bestStreakCount Current streak count of the top habit.
 */
data class OverviewStats(
    val completionsToday: Int,
    val rate7d: Double?,
    val rate30d: Double?,
    val bestStreakHabitName: String?,
    val bestStreakCount: Int
)
