package com.tidemark.habits.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tidemark.habits.domain.logic.ScheduleEvaluator
import com.tidemark.habits.domain.logic.StatsCalculator
import com.tidemark.habits.domain.logic.StreakCalculator
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.Schedule
import com.tidemark.habits.domain.model.StreakResult
import com.tidemark.habits.domain.repo.HabitRepository
import com.tidemark.habits.domain.time.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HabitListItem(
    val habit: Habit,
    val streak: StreakResult,
    val rate30d: Float?,
    val scheduleSummary: String,
    val reminderSummary: String?
)

data class HabitsUiState(
    val habits: List<HabitListItem> = emptyList(),
    val isLoading: Boolean = false,
    val habitToDelete: Habit? = null
)

class HabitsViewModel(
    private val repository: HabitRepository,
    private val onCancelReminder: (Long) -> Unit,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    init {
        observeHabits()
    }

    private fun observeHabits() {
        viewModelScope.launch {
            val today = timeProvider.today()
            // Observe habits and logs for streak & 30-day stats
            combine(
                repository.observeHabits(),
                repository.observeAllLogs()
            ) { habits, logs ->
                val logsByHabit = logs.groupBy { it.habitId }
                habits.map { habit ->
                    val habitLogs = logsByHabit[habit.id] ?: emptyList()
                    val streak = StreakCalculator.calculateStreak(habit, habitLogs, today)
                    val stats = StatsCalculator.calculateStats(habit, habitLogs, today)

                    val scheduleSummary = when (val s = habit.schedule) {
                        is Schedule.Daily -> "Every day"
                        is Schedule.Weekdays -> s.days.sorted().joinToString(" · ") {
                            it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }
                        }
                        is Schedule.TimesPerWeek -> "${s.times}× per week"
                    }

                    val reminderSummary = habit.reminderTime?.format(
                        DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                    )

                    HabitListItem(
                        habit = habit,
                        streak = streak,
                        rate30d = stats.rate30d,
                        scheduleSummary = scheduleSummary,
                        reminderSummary = reminderSummary
                    )
                }
            }.collect { items ->
                _uiState.value = _uiState.value.copy(habits = items)
            }
        }
    }

    fun requestDelete(habit: Habit) {
        _uiState.value = _uiState.value.copy(habitToDelete = habit)
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(habitToDelete = null)
    }

    fun confirmDelete() {
        val habit = _uiState.value.habitToDelete ?: return
        viewModelScope.launch {
            onCancelReminder(habit.id)
            repository.deleteHabit(habit.id)
            _uiState.value = _uiState.value.copy(habitToDelete = null)
        }
    }
}
