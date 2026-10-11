package com.tidemark.habits.domain.model

/**
 * Progress tracking for the current ISO week.
 *
 * @property done Number of completed days this week.
 * @property required Goal completions required this week.
 */
data class WeekProgress(
    val done: Int,
    val required: Int
)
