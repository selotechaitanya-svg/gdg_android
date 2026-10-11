package com.tidemark.habits.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tidemark.habits.domain.logic.LogEditor
import com.tidemark.habits.domain.logic.StatsCalculator
import com.tidemark.habits.domain.logic.StreakCalculator
import com.tidemark.habits.domain.model.DayInfo
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitStats
import com.tidemark.habits.domain.model.StreakResult
import com.tidemark.habits.domain.model.WeeklyBar
import com.tidemark.habits.domain.repo.HabitRepository
import com.tidemark.habits.domain.time.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HabitDetailUiState(
    val habit: Habit? = null,
    val streak: StreakResult? = null,
    val stats: HabitStats? = null,
    val yearMonth: YearMonth,
    val monthDays: List<DayInfo> = emptyList(),
    val weeklyBars: List<WeeklyBar> = emptyList(),
    val selectedDayToEdit: DayInfo? = null,
    val isLoading: Boolean = true
)

class HabitDetailViewModel(
    private val habitId: Long,
    private val repository: HabitRepository,
    private val timeProvider: TimeProvider,
    private val onCancelReminder: (Long) -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HabitDetailUiState(
            yearMonth = YearMonth.from(timeProvider.today())
        )
    )
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    init {
        observeHabitDetails()
    }

    private fun observeHabitDetails() {
        viewModelScope.launch {
            repository.observeLogs(habitId).collect { logs ->
                val habit = repository.getHabit(habitId)
                if (habit != null) {
                    val today = timeProvider.today()
                    val streak = StreakCalculator.calculateStreak(habit, logs, today)
                    val stats = StatsCalculator.calculateHabitStats(habit, logs, today)
                    val monthDays = StatsCalculator.calculateMonthDays(
                        habit = habit,
                        logs = logs,
                        yearMonth = _uiState.value.yearMonth,
                        today = today
                    )
                    val bars = StatsCalculator.calculateWeeklyBars(habit, logs, today)

                    _uiState.update {
                        it.copy(
                            habit = habit,
                            streak = streak,
                            stats = stats,
                            monthDays = monthDays,
                            weeklyBars = bars,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun previousMonth() {
        _uiState.update { it.copy(yearMonth = it.yearMonth.minusMonths(1)) }
        recalculateMonthDays()
    }

    fun nextMonth() {
        _uiState.update { it.copy(yearMonth = it.yearMonth.plusMonths(1)) }
        recalculateMonthDays()
    }

    private fun recalculateMonthDays() {
        val habit = _uiState.value.habit ?: return
        viewModelScope.launch {
            val logs = repository.getLogs(habitId)
            val days = StatsCalculator.calculateMonthDays(
                habit = habit,
                logs = logs,
                yearMonth = _uiState.value.yearMonth,
                today = timeProvider.today()
            )
            _uiState.update { it.copy(monthDays = days) }
        }
    }

    fun selectDayToEdit(dayInfo: DayInfo) {
        val habit = _uiState.value.habit ?: return
        val today = timeProvider.today()
        // Allow correcting any day from startDate up to today
        if (!dayInfo.date.isBefore(habit.startDate) && !dayInfo.date.isAfter(today)) {
            _uiState.update { it.copy(selectedDayToEdit = dayInfo) }
        }
    }

    fun dismissEditDay() {
        _uiState.update { it.copy(selectedDayToEdit = null) }
    }

    fun saveDayLog(value: Int) {
        val habit = _uiState.value.habit ?: return
        val day = _uiState.value.selectedDayToEdit ?: return
        val today = timeProvider.today()

        viewModelScope.launch {
            val editResult = LogEditor.evaluate(habit, day.date, value, today)
            when (editResult) {
                is com.tidemark.habits.domain.logic.LogEditResult.Upsert -> {
                    repository.setLog(habit.id, day.date, editResult.log.value, editResult.log.completed)
                }
                is com.tidemark.habits.domain.logic.LogEditResult.Clear -> {
                    repository.clearLog(habit.id, day.date)
                }
                is com.tidemark.habits.domain.logic.LogEditResult.Error -> Unit
            }
            _uiState.update { it.copy(selectedDayToEdit = null) }
        }
    }

    fun deleteHabit(onDeleted: () -> Unit) {
        viewModelScope.launch {
            onCancelReminder(habitId)
            repository.deleteHabit(habitId)
            onDeleted()
        }
    }
}
