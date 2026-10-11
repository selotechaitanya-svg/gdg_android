package com.tidemark.habits.domain.logic

import com.tidemark.habits.domain.model.DayInfo
import com.tidemark.habits.domain.model.DayState
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitStats
import com.tidemark.habits.domain.model.Schedule
import com.tidemark.habits.domain.model.WeekProgress
import com.tidemark.habits.domain.model.WeeklyBar
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure statistics engine computing completion rates, calendar DayStates,
 * weekly progress, and chart metrics across multiple time windows.
 */
object StatsCalculator {

    /**
     * Calculates comprehensive statistics for a single [habit].
     */
    fun calculateHabitStats(
        habit: Habit,
        logs: List<HabitLog>,
        today: LocalDate
    ): HabitStats {
        val streak = StreakCalculator.calculateStreak(habit, logs, today)
        val validLogs = logs.filter { !it.date.isBefore(habit.startDate) && !it.date.isAfter(today) }
        val totalCompletions = validLogs.count { it.completed }
        val thisWeek = calculateThisWeekProgress(habit, logs, today)

        val rate7d: Double?
        val rate30d: Double?
        val rateAllTime: Double?

        when (val schedule = habit.schedule) {
            is Schedule.Daily, is Schedule.Weekdays -> {
                rate7d = calculateDayRate(habit, logs, windowDays = 7, today = today)
                rate30d = calculateDayRate(habit, logs, windowDays = 30, today = today)
                rateAllTime = calculateDayRate(habit, logs, windowDays = null, today = today)
            }
            is Schedule.TimesPerWeek -> {
                rate7d = calculateWeekRate(habit, schedule.times, logs, windowWeeks = 4, today = today)
                rate30d = calculateWeekRate(habit, schedule.times, logs, windowWeeks = 12, today = today)
                rateAllTime = calculateWeekRate(habit, schedule.times, logs, windowWeeks = null, today = today)
            }
        }

        return HabitStats(
            habitId = habit.id,
            currentStreak = streak.currentStreak,
            longestStreak = streak.longestStreak,
            completionRate7d = rate7d,
            completionRate30d = rate30d,
            completionRateAllTime = rateAllTime,
            thisWeekProgress = thisWeek,
            totalCompletions = totalCompletions
        )
    }

    /**
     * Computes the current week's completion count and target.
     */
    fun calculateThisWeekProgress(
        habit: Habit,
        logs: List<HabitLog>,
        today: LocalDate
    ): WeekProgress {
        val weekStart = WeekUtils.startOfWeek(today)
        val weekEnd = WeekUtils.endOfWeek(today)

        val validLogs = logs.filter {
            !it.date.isBefore(habit.startDate) &&
                    !it.date.isAfter(today) &&
                    !it.date.isBefore(weekStart) &&
                    !it.date.isAfter(weekEnd)
        }

        return when (val schedule = habit.schedule) {
            is Schedule.TimesPerWeek -> {
                val done = validLogs.filter { it.completed }.map { it.date }.distinct().size
                val isFirstWeek = weekStart == WeekUtils.startOfWeek(habit.startDate)
                val required = if (isFirstWeek) {
                    minOf(schedule.times, WeekUtils.daysRemainingInWeek(habit.startDate))
                } else {
                    schedule.times
                }
                WeekProgress(done = done, required = required)
            }
            is Schedule.Daily, is Schedule.Weekdays -> {
                val done = validLogs.filter {
                    it.completed && ScheduleEvaluator.isScheduled(habit.schedule, it.date)
                }.map { it.date }.distinct().size

                var scheduledCount = 0
                var curr = maxOf(habit.startDate, weekStart)
                while (!curr.isAfter(weekEnd)) {
                    if (ScheduleEvaluator.isScheduled(habit.schedule, curr)) {
                        scheduledCount++
                    }
                    curr = curr.plusDays(1)
                }
                WeekProgress(done = done, required = scheduledCount)
            }
        }
    }

    /**
     * Computes completion rate for Daily / Weekdays habits.
     * Excludes a pending (uncompleted) today from the denominator.
     */
    fun calculateDayRate(
        habit: Habit,
        logs: List<HabitLog>,
        windowDays: Int?,
        today: LocalDate
    ): Double? {
        if (habit.startDate.isAfter(today)) return null

        val windowStart = if (windowDays != null) {
            val calcStart = today.minusDays((windowDays - 1).toLong())
            if (calcStart.isBefore(habit.startDate)) habit.startDate else calcStart
        } else {
            habit.startDate
        }

        if (windowStart.isAfter(today)) return null

        val logsByDate = logs.associateBy { it.date }
        var scheduledDays = 0
        var completedDays = 0

        var curr = windowStart
        while (!curr.isAfter(today)) {
            if (ScheduleEvaluator.isScheduled(habit.schedule, curr)) {
                val isComplete = logsByDate[curr]?.completed == true
                if (isComplete) {
                    scheduledDays++
                    completedDays++
                } else if (curr == today) {
                    // Pending today is excluded from the denominator
                } else {
                    scheduledDays++
                }
            }
            curr = curr.plusDays(1)
        }

        if (scheduledDays == 0) return null
        return (completedDays.toDouble() / scheduledDays.toDouble()) * 100.0
    }

    /**
     * Computes completion rate for TimesPerWeek habits.
     * Excludes an unmet in-progress current week from evaluated weeks.
     */
    fun calculateWeekRate(
        habit: Habit,
        timesPerWeek: Int,
        logs: List<HabitLog>,
        windowWeeks: Int?,
        today: LocalDate
    ): Double? {
        if (habit.startDate.isAfter(today)) return null

        val allWeeks = WeekUtils.weeksBetween(habit.startDate, today)
        if (allWeeks.isEmpty()) return null

        val weeks = if (windowWeeks != null && allWeeks.size > windowWeeks) {
            allWeeks.takeLast(windowWeeks)
        } else {
            allWeeks
        }

        val completedDates = logs.filter { it.completed && !it.date.isBefore(habit.startDate) && !it.date.isAfter(today) }
            .map { it.date }
            .toSet()

        val currentWeekMonday = WeekUtils.startOfWeek(today)
        val firstWeekMonday = WeekUtils.startOfWeek(habit.startDate)

        var evaluatedWeeks = 0
        var metWeeks = 0

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

            val isMet = doneInWeek >= required
            if (isMet) {
                evaluatedWeeks++
                metWeeks++
            } else if (monday == currentWeekMonday) {
                // In-progress unmet current week is excluded
            } else {
                evaluatedWeeks++
            }
        }

        if (evaluatedWeeks == 0) return null
        return (metWeeks.toDouble() / evaluatedWeeks.toDouble()) * 100.0
    }

    /**
     * Generates calendar [DayInfo] models for all days in [yearMonth].
     */
    fun calculateMonthDays(
        habit: Habit,
        logs: List<HabitLog>,
        yearMonth: YearMonth,
        today: LocalDate
    ): List<DayInfo> {
        val logsByDate = logs.associateBy { it.date }
        val daysInMonth = yearMonth.lengthOfMonth()
        val result = ArrayList<DayInfo>(daysInMonth)

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val log = logsByDate[date]
            val value = log?.value ?: 0
            val isComplete = log?.completed == true

            val state: DayState
            var isBonus = false

            if (date.isBefore(habit.startDate)) {
                state = DayState.BEFORE_START
            } else if (date.isAfter(today)) {
                state = DayState.FUTURE
            } else {
                when (habit.schedule) {
                    is Schedule.TimesPerWeek -> {
                        state = when {
                            isComplete -> DayState.COMPLETE
                            value > 0 -> DayState.PARTIAL
                            else -> DayState.NEUTRAL
                        }
                    }
                    is Schedule.Daily, is Schedule.Weekdays -> {
                        val isScheduled = ScheduleEvaluator.isScheduled(habit.schedule, date)
                        if (!isScheduled) {
                            state = DayState.UNSCHEDULED
                            if (value > 0) isBonus = true
                        } else {
                            state = when {
                                isComplete -> DayState.COMPLETE
                                value > 0 -> DayState.PARTIAL
                                date == today -> DayState.PENDING
                                else -> DayState.MISSED
                            }
                        }
                    }
                }
            }

            result.add(
                DayInfo(
                    date = date,
                    state = state,
                    value = value,
                    isBonus = isBonus
                )
            )
        }

        return result
    }

    /**
     * Generates weekly bar data for the last [weeksCount] ISO weeks ending at [today].
     */
    fun calculateWeeklyBars(
        habit: Habit,
        logs: List<HabitLog>,
        today: LocalDate,
        weeksCount: Int = 12
    ): List<WeeklyBar> {
        val currentWeekMonday = WeekUtils.startOfWeek(today)
        val firstWeekMonday = currentWeekMonday.minusWeeks((weeksCount - 1).toLong())

        val weeks = WeekUtils.weeksBetween(firstWeekMonday, currentWeekMonday)
        val completedDates = logs.filter { it.completed && !it.date.isBefore(habit.startDate) && !it.date.isAfter(today) }
            .map { it.date }
            .toSet()

        return weeks.map { monday ->
            val sunday = WeekUtils.endOfWeek(monday)
            val doneInWeek = completedDates.count { !it.isBefore(monday) && !it.isAfter(sunday) }

            val required = when (val schedule = habit.schedule) {
                is Schedule.TimesPerWeek -> {
                    if (monday == WeekUtils.startOfWeek(habit.startDate)) {
                        minOf(schedule.times, WeekUtils.daysRemainingInWeek(habit.startDate))
                    } else {
                        schedule.times
                    }
                }
                is Schedule.Daily, is Schedule.Weekdays -> {
                    var count = 0
                    var curr = maxOf(habit.startDate, monday)
                    while (!curr.isAfter(sunday)) {
                        if (ScheduleEvaluator.isScheduled(habit.schedule, curr)) {
                            count++
                        }
                        curr = curr.plusDays(1)
                    }
                    count
                }
            }

            val percentage = if (required > 0) {
                minOf(100.0, (doneInWeek.toDouble() / required.toDouble()) * 100.0)
            } else {
                0.0
            }

            WeeklyBar(
                weekStart = monday,
                done = doneInWeek,
                required = required,
                percentage = percentage
            )
        }
    }
}
