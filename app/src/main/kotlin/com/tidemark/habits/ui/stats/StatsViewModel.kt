package com.tidemark.habits.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tidemark.habits.domain.logic.OverviewCalculator
import com.tidemark.habits.domain.logic.ScheduleEvaluator
import com.tidemark.habits.domain.logic.StatsCalculator
import com.tidemark.habits.domain.logic.StreakCalculator
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.OverviewStats
import com.tidemark.habits.domain.model.StreakResult
import com.tidemark.habits.domain.repo.HabitRepository
import com.tidemark.habits.domain.time.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class Daily7dBar(
    val date: LocalDate,
    val completedCount: Int,
    val scheduledCount: Int,
    val percentage: Float
)

data class HabitStatsRow(
    val habit: Habit,
    val streak: StreakResult,
    val rate30d: Float?,
    val totalCompletions: Int
)

data class StatsUiState(
    val overview: OverviewStats = OverviewStats(0, null, null, null, 0),
    val daily7dBars: List<Daily7dBar> = emptyList(),
    val habitRows: List<HabitStatsRow> = emptyList(),
    val hasHabits: Boolean = false,
    val isLoading: Boolean = true
)

class StatsViewModel(
    private val repository: HabitRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        observeStats()
    }

    private fun observeStats() {
        viewModelScope.launch {
            val today = timeProvider.today()
            val start7d = today.minusDays(6)
            val start60d = today.minusDays(60)

            combine(
                repository.observeHabits(),
                repository.observeLogsBetween(start60d, today)
            ) { habits, logs ->
                if (habits.isEmpty()) {
                    StatsUiState(hasHabits = false, isLoading = false)
                } else {
                    val overview = OverviewCalculator.calculateOverview(habits, logs, today)
                    val logsByHabit = logs.groupBy { it.habitId }

                    // Daily 7-day completion bars
                    val logsByDate = logs.groupBy { it.date }
                    val dailyBars = (6 downTo 0).map { offset ->
                        val date = today.minusDays(offset.toLong())
                        val scheduled = habits.count { habit ->
                            !date.isBefore(habit.startDate) && ScheduleEvaluator.isScheduled(habit.schedule, date)
                        }
                        val completed = logsByDate[date]?.count { it.completed } ?: 0
                        val pct = if (scheduled > 0) {
                            (completed.toFloat() / scheduled.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        Daily7dBar(
                            date = date,
                            completedCount = completed,
                            scheduledCount = scheduled,
                            percentage = pct
                        )
                    }

                    // Per-habit rows sorted by current streak descending
                    val rows = habits.map { habit ->
                        val habitLogs = logsByHabit[habit.id] ?: emptyList()
                        val streak = StreakCalculator.calculateStreak(habit, habitLogs, today)
                        val stats = StatsCalculator.calculateHabitStats(habit, habitLogs, today)
                        HabitStatsRow(
                            habit = habit,
                            streak = streak,
                            rate30d = stats.completionRate30d?.toFloat()?.div(100f),
                            totalCompletions = stats.totalCompletions
                        )
                    }.sortedByDescending { it.streak.currentStreak }

                    StatsUiState(
                        overview = overview,
                        daily7dBars = dailyBars,
                        habitRows = rows,
                        hasHabits = true,
                        isLoading = false
                    )
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
}
