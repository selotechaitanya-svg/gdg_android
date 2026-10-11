package com.tidemark.habits.domain.model

import java.time.LocalDate

/**
 * Historical weekly progress for habit charts.
 *
 * @property weekStart Monday date of the week.
 * @property done Count of completed days or metric achieved.
 * @property required Required goal count.
 * @property percentage Completion percentage in [0.0, 100.0].
 */
data class WeeklyBar(
    val weekStart: LocalDate,
    val done: Int,
    val required: Int,
    val percentage: Double
)
