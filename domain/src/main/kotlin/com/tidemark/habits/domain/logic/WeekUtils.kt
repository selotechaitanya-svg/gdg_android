package com.tidemark.habits.domain.logic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

/**
 * Pure utility functions for ISO-8601 calendar week calculations.
 * Weeks strictly start on Monday and end on Sunday.
 */
object WeekUtils {
    /** Standard ISO-8601 week fields starting on Monday. */
    val ISO_WEEK_FIELDS: WeekFields = WeekFields.ISO

    /**
     * Returns the Monday starting the ISO week containing [date].
     */
    fun startOfWeek(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    /**
     * Returns the Sunday ending the ISO week containing [date].
     */
    fun endOfWeek(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    }

    /**
     * Number of calendar days from [startDate] to the Sunday of that same week, inclusive.
     * Example: Starting on Saturday returns 2 (Saturday + Sunday).
     */
    fun daysRemainingInWeek(startDate: LocalDate): Int {
        val sunday = endOfWeek(startDate)
        return ChronoUnit.DAYS.between(startDate, sunday).toInt() + 1
    }

    /**
     * Generates all start-of-week Mondays between [from] and [to] inclusive.
     */
    fun weeksBetween(from: LocalDate, to: LocalDate): List<LocalDate> {
        val startMonday = startOfWeek(from)
        val endMonday = startOfWeek(to)
        if (startMonday.isAfter(endMonday)) return emptyList()

        val weeks = mutableListOf<LocalDate>()
        var curr = startMonday
        while (!curr.isAfter(endMonday)) {
            weeks.add(curr)
            curr = curr.plusWeeks(1)
        }
        return weeks
    }
}
