package com.tidemark.habits.domain.model

/**
 * Classification of a date for a habit.
 */
enum class DayState {
    /** Scheduled and fully completed (value >= target). */
    COMPLETE,
    /** Scheduled with non-zero progress, but less than target. */
    PARTIAL,
    /** Scheduled, prior to today, and not completed. */
    MISSED,
    /** Scheduled for today, but not yet completed (still in progress). */
    PENDING,
    /** Day is not on the schedule for Daily/Weekdays. */
    UNSCHEDULED,
    /** Past non-completed day for TimesPerWeek (weekly goal, not daily). */
    NEUTRAL,
    /** Date falls before the habit's startDate. */
    BEFORE_START,
    /** Date is after today (cannot be logged yet). */
    FUTURE
}
