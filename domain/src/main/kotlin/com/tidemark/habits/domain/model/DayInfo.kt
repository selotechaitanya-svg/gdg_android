package com.tidemark.habits.domain.model

import java.time.LocalDate

/**
 * Complete evaluation of a habit on a single calendar day.
 *
 * @property date Evaluated local date.
 * @property state Status for UI display and streaks.
 * @property value Progress recorded on this day (0 if unlogged).
 * @property isBonus True if logged on an unscheduled day.
 */
data class DayInfo(
    val date: LocalDate,
    val state: DayState,
    val value: Int,
    val isBonus: Boolean = false
)
