package com.tidemark.habits.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tidemark.habits.data.settings.ActiveTimer
import com.tidemark.habits.data.settings.SettingsRepository
import com.tidemark.habits.domain.logic.LogEditor
import com.tidemark.habits.domain.logic.ProgressEvaluator
import com.tidemark.habits.domain.logic.ScheduleEvaluator
import com.tidemark.habits.domain.logic.StatsCalculator
import com.tidemark.habits.domain.logic.StreakCalculator
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import com.tidemark.habits.domain.model.StreakResult
import com.tidemark.habits.domain.model.WeekProgress
import com.tidemark.habits.domain.repo.HabitRepository
import com.tidemark.habits.domain.time.TimeProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayHabitItem(
    val habit: Habit,
    val log: HabitLog?,
    val streak: StreakResult,
    val isCompleted: Boolean,
    val progressValue: Int,
    val weekProgress: WeekProgress?,
    val isWeekGoalMet: Boolean
)

data class DateStripItem(
    val date: LocalDate,
    val isAllCompleted: Boolean,
    val isSelected: Boolean
)

data class TodayUiState(
    val selectedDate: LocalDate,
    val currentDate: LocalDate,
    val dateStrip: List<DateStripItem> = emptyList(),
    val items: List<TodayHabitItem> = emptyList(),
    val completedCount: Int = 0,
    val totalScheduledCount: Int = 0,
    val activeTimer: ActiveTimer? = null,
    val elapsedTimerSeconds: Long = 0,
    val lastToggledHabitId: Long? = null,
    val lastToggledPreviousValue: Int? = null
)

class TodayViewModel(
    private val repository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(timeProvider.today())
    private val _uiState = MutableStateFlow(
        TodayUiState(
            selectedDate = timeProvider.today(),
            currentDate = timeProvider.today()
        )
    )
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        observeData()
        startStopwatchTicker()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.observeHabits(),
                repository.observeAllLogs(),
                _selectedDate,
                settingsRepository.activeTimerFlow
            ) { habits, logs, selectedDate, activeTimer ->
                buildUiState(habits, logs, selectedDate, activeTimer)
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun buildUiState(
        habits: List<Habit>,
        logs: List<HabitLog>,
        selectedDate: LocalDate,
        activeTimer: ActiveTimer?
    ): TodayUiState {
        val today = timeProvider.today()
        val logsByHabitAndDate = logs.associateBy { Pair(it.habitId, it.date) }
        val logsByHabit = logs.groupBy { it.habitId }

        // Date strip for last 14 days
        val stripItems = (13 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val scheduledForDate = habits.filter { habit ->
                !date.isBefore(habit.startDate) && ScheduleEvaluator.isScheduled(habit.schedule, date)
            }
            val allDone = scheduledForDate.isNotEmpty() && scheduledForDate.all { habit ->
                logsByHabitAndDate[Pair(habit.id, date)]?.completed == true
            }
            DateStripItem(
                date = date,
                isAllCompleted = allDone,
                isSelected = date == selectedDate
            )
        }

        // Filter habits active on selectedDate
        val items = habits.filter { habit ->
            !selectedDate.isBefore(habit.startDate) && when (habit.schedule) {
                is Schedule.Daily, is Schedule.Weekdays -> ScheduleEvaluator.isScheduled(habit.schedule, selectedDate)
                is Schedule.TimesPerWeek -> true
            }
        }.map { habit ->
            val log = logsByHabitAndDate[Pair(habit.id, selectedDate)]
            val habitLogs = logsByHabit[habit.id] ?: emptyList()
            val streak = StreakCalculator.calculateStreak(habit, habitLogs, selectedDate)
            val weekProgress = StatsCalculator.calculateThisWeekProgress(habit, habitLogs, selectedDate)
            val isWeekGoalMet = habit.schedule is Schedule.TimesPerWeek &&
                    weekProgress.done >= weekProgress.required

            val progressValue = log?.value ?: 0
            val isCompleted = log?.completed == true

            TodayHabitItem(
                habit = habit,
                log = log,
                streak = streak,
                isCompleted = isCompleted,
                progressValue = progressValue,
                weekProgress = weekProgress,
                isWeekGoalMet = isWeekGoalMet
            )
        }

        // Summary x of y done
        // N-per-week habits: excluded from day's x of y unless completed that day, or if week goal already met
        val countableForDay = items.filter { item ->
            if (item.habit.schedule is Schedule.TimesPerWeek) {
                item.isCompleted || !item.isWeekGoalMet
            } else {
                true
            }
        }
        val completedCount = countableForDay.count { it.isCompleted }
        val totalScheduledCount = countableForDay.size

        val elapsed = if (activeTimer != null) {
            val nowMs = timeProvider.now().toEpochMilli()
            ((nowMs - activeTimer.startEpochMs) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }

        return TodayUiState(
            selectedDate = selectedDate,
            currentDate = today,
            dateStrip = stripItems,
            items = items,
            completedCount = completedCount,
            totalScheduledCount = totalScheduledCount,
            activeTimer = activeTimer,
            elapsedTimerSeconds = elapsed,
            lastToggledHabitId = _uiState.value.lastToggledHabitId,
            lastToggledPreviousValue = _uiState.value.lastToggledPreviousValue
        )
    }

    private fun startStopwatchTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val timer = _uiState.value.activeTimer
                if (timer != null) {
                    val nowMs = timeProvider.now().toEpochMilli()
                    val seconds = ((nowMs - timer.startEpochMs) / 1000L).coerceAtLeast(0L)
                    _uiState.update { it.copy(elapsedTimerSeconds = seconds) }
                }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        val today = timeProvider.today()
        if (!date.isAfter(today)) {
            _selectedDate.value = date
        }
    }

    fun toggleYesNo(habit: Habit) {
        viewModelScope.launch {
            val currentLog = repository.getLog(habit.id, _selectedDate.value)
            val prevValue = currentLog?.value ?: 0
            val newValue = if (prevValue > 0) 0 else 1

            _uiState.update {
                it.copy(
                    lastToggledHabitId = habit.id,
                    lastToggledPreviousValue = prevValue
                )
            }

            if (newValue == 0) {
                repository.clearLog(habit.id, _selectedDate.value)
            } else {
                repository.setLog(habit.id, _selectedDate.value, newValue, completed = true)
            }
        }
    }

    fun undoLastToggle() {
        val habitId = _uiState.value.lastToggledHabitId ?: return
        val prevValue = _uiState.value.lastToggledPreviousValue ?: return

        viewModelScope.launch {
            if (prevValue == 0) {
                repository.clearLog(habitId, _selectedDate.value)
            } else {
                repository.setLog(habitId, _selectedDate.value, prevValue, completed = true)
            }
            _uiState.update { it.copy(lastToggledHabitId = null, lastToggledPreviousValue = null) }
        }
    }

    fun quickAdd(habit: Habit) {
        viewModelScope.launch {
            val currentLog = repository.getLog(habit.id, _selectedDate.value)
            val currentVal = currentLog?.value ?: 0
            val newVal = currentVal + habit.step
            val completed = ProgressEvaluator.isComplete(habit, newVal)
            repository.setLog(habit.id, _selectedDate.value, newVal, completed)
        }
    }

    fun setLogValue(habit: Habit, value: Int) {
        viewModelScope.launch {
            val editResult = LogEditor.evaluate(habit, _selectedDate.value, value, timeProvider.today())
            when (editResult) {
                is com.tidemark.habits.domain.logic.LogEditResult.Upsert -> {
                    repository.setLog(habit.id, _selectedDate.value, editResult.log.value, editResult.log.completed)
                }
                is com.tidemark.habits.domain.logic.LogEditResult.Clear -> {
                    repository.clearLog(habit.id, _selectedDate.value)
                }
                is com.tidemark.habits.domain.logic.LogEditResult.Error -> Unit
            }
        }
    }

    fun startStopwatch(habitId: Long) {
        viewModelScope.launch {
            val nowMs = timeProvider.now().toEpochMilli()
            settingsRepository.startTimer(habitId, nowMs, _selectedDate.value)
        }
    }

    fun stopStopwatch(habit: Habit) {
        viewModelScope.launch {
            val timer = _uiState.value.activeTimer ?: return@launch
            if (timer.habitId != habit.id) return@launch

            val nowMs = timeProvider.now().toEpochMilli()
            val totalSeconds = ((nowMs - timer.startEpochMs) / 1000L).coerceAtLeast(0L)

            // Whole minutes: >= 30 s rounds up, < 30 s dropped
            val minutes = if (totalSeconds % 60 >= 30) {
                (totalSeconds / 60) + 1
            } else {
                totalSeconds / 60
            }.toInt()

            settingsRepository.clearTimer()

            if (minutes > 0) {
                // Credit whole minutes to the date the session STARTED
                val currentLog = repository.getLog(habit.id, timer.startDate)
                val currentVal = currentLog?.value ?: 0
                val newVal = currentVal + minutes
                val completed = ProgressEvaluator.isComplete(habit, newVal)
                repository.setLog(habit.id, timer.startDate, newVal, completed)
            }
        }
    }

    fun addManualMinutes(habit: Habit, minutes: Int) {
        viewModelScope.launch {
            val currentLog = repository.getLog(habit.id, _selectedDate.value)
            val currentVal = currentLog?.value ?: 0
            val newVal = currentVal + minutes
            val completed = ProgressEvaluator.isComplete(habit, newVal)
            repository.setLog(habit.id, _selectedDate.value, newVal, completed)
        }
    }

    fun onDateChanged() {
        val newToday = timeProvider.today()
        if (_uiState.value.currentDate != newToday) {
            _selectedDate.value = newToday
            _uiState.update { it.copy(currentDate = newToday, selectedDate = newToday) }
        }
    }
}
