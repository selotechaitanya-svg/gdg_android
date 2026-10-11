package com.tidemark.habits.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tidemark.habits.domain.logic.HabitValidator
import com.tidemark.habits.domain.logic.HabitValidationError
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import com.tidemark.habits.domain.repo.HabitRepository
import com.tidemark.habits.domain.time.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class HabitEditorUiState(
    val habitId: Long? = null,
    val name: String = "",
    val type: HabitType = HabitType.YES_NO,
    val target: Int = 1,
    val unit: String = "",
    val step: Int = 1,
    val scheduleTypeIndex: Int = 0, // 0 = Daily, 1 = Weekdays, 2 = TimesPerWeek
    val weekdays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
    val timesPerWeek: Int = 3,
    val hasReminder: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(20, 0),
    val startDate: LocalDate,
    val originalHabit: Habit? = null,
    val validationErrors: List<HabitValidationError> = emptyList(),
    val showTypeChangeConfirmation: Boolean = false,
    val pendingTypeChange: HabitType? = null,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false
) {
    val isValid: Boolean get() = validationErrors.isEmpty() && name.isNotBlank()
}

class HabitEditorViewModel(
    private val habitId: Long?,
    prefillName: String?,
    private val repository: HabitRepository,
    private val timeProvider: TimeProvider,
    private val onScheduleReminder: (Habit) -> Unit,
    private val onCancelReminder: (Long) -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HabitEditorUiState(
            habitId = habitId,
            name = prefillName ?: "",
            startDate = timeProvider.today(),
            isLoading = habitId != null
        )
    )
    val uiState: StateFlow<HabitEditorUiState> = _uiState.asStateFlow()

    init {
        if (habitId != null) {
            loadHabit(habitId)
        } else {
            revalidate()
        }
    }

    private fun loadHabit(id: Long) {
        viewModelScope.launch {
            val habit = repository.getHabit(id)
            if (habit != null) {
                val scheduleIndex = when (habit.schedule) {
                    is Schedule.Daily -> 0
                    is Schedule.Weekdays -> 1
                    is Schedule.TimesPerWeek -> 2
                }
                val weekdays = when (val s = habit.schedule) {
                    is Schedule.Weekdays -> s.days
                    else -> setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
                }
                val times = when (val s = habit.schedule) {
                    is Schedule.TimesPerWeek -> s.times
                    else -> 3
                }

                _uiState.update {
                    it.copy(
                        name = habit.name,
                        type = habit.type,
                        target = habit.target,
                        unit = habit.unit ?: "",
                        step = habit.step,
                        scheduleTypeIndex = scheduleIndex,
                        weekdays = weekdays,
                        timesPerWeek = times,
                        hasReminder = habit.reminderTime != null,
                        reminderTime = habit.reminderTime ?: LocalTime.of(20, 0),
                        startDate = habit.startDate,
                        originalHabit = habit,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
            revalidate()
        }
    }

    private fun buildCandidateHabit(state: HabitEditorUiState): Habit {
        val schedule = when (state.scheduleTypeIndex) {
            0 -> Schedule.Daily
            1 -> Schedule.Weekdays(state.weekdays)
            2 -> Schedule.TimesPerWeek(state.timesPerWeek)
            else -> Schedule.Daily
        }
        val target = when (state.type) {
            HabitType.YES_NO -> 1
            HabitType.COUNTABLE, HabitType.TIMED -> state.target.coerceAtLeast(1)
        }
        val unit = when (state.type) {
            HabitType.YES_NO -> null
            HabitType.COUNTABLE -> state.unit.trim().ifEmpty { null }
            HabitType.TIMED -> "min"
        }

        return Habit(
            id = state.habitId ?: 0L,
            name = state.name.trim(),
            type = state.type,
            schedule = schedule,
            target = target,
            unit = unit,
            step = state.step.coerceAtLeast(1),
            reminderTime = if (state.hasReminder) state.reminderTime else null,
            startDate = state.startDate,
            createdAt = state.originalHabit?.createdAt ?: timeProvider.now()
        )
    }

    private fun revalidate() {
        val candidate = buildCandidateHabit(_uiState.value)
        val errors = HabitValidator.validate(candidate, timeProvider.today())
        _uiState.update { it.copy(validationErrors = errors) }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
        revalidate()
    }

    fun requestTypeChange(newType: HabitType) {
        val currentType = _uiState.value.type
        if (currentType == newType) return

        // If editing an existing habit with history, show confirmation dialog
        if (_uiState.value.originalHabit != null && _uiState.value.originalHabit?.type != newType) {
            _uiState.update {
                it.copy(
                    showTypeChangeConfirmation = true,
                    pendingTypeChange = newType
                )
            }
        } else {
            applyTypeChange(newType)
        }
    }

    fun confirmTypeChange() {
        val pending = _uiState.value.pendingTypeChange ?: return
        applyTypeChange(pending)
        _uiState.update { it.copy(showTypeChangeConfirmation = false, pendingTypeChange = null) }
    }

    fun cancelTypeChange() {
        _uiState.update { it.copy(showTypeChangeConfirmation = false, pendingTypeChange = null) }
    }

    private fun applyTypeChange(newType: HabitType) {
        _uiState.update { current ->
            val defaultTarget = when (newType) {
                HabitType.YES_NO -> 1
                HabitType.COUNTABLE -> if (current.target == 1) 20 else current.target
                HabitType.TIMED -> if (current.target == 1) 25 else current.target
            }
            val defaultUnit = when (newType) {
                HabitType.YES_NO -> ""
                HabitType.COUNTABLE -> current.unit.ifEmpty { "pages" }
                HabitType.TIMED -> "min"
            }
            current.copy(
                type = newType,
                target = defaultTarget,
                unit = defaultUnit
            )
        }
        revalidate()
    }

    fun updateTarget(target: Int) {
        _uiState.update { it.copy(target = target) }
        revalidate()
    }

    fun updateUnit(unit: String) {
        _uiState.update { it.copy(unit = unit) }
        revalidate()
    }

    fun updateStep(step: Int) {
        _uiState.update { it.copy(step = step) }
        revalidate()
    }

    fun updateScheduleTypeIndex(index: Int) {
        _uiState.update { it.copy(scheduleTypeIndex = index) }
        revalidate()
    }

    fun toggleWeekday(day: DayOfWeek) {
        _uiState.update { current ->
            val set = current.weekdays.toMutableSet()
            if (set.contains(day)) {
                if (set.size > 1) set.remove(day) // Keep at least one
            } else {
                set.add(day)
            }
            current.copy(weekdays = set)
        }
        revalidate()
    }

    fun updateTimesPerWeek(times: Int) {
        _uiState.update { it.copy(timesPerWeek = times.coerceIn(1, 7)) }
        revalidate()
    }

    fun toggleReminder(enabled: Boolean) {
        _uiState.update { it.copy(hasReminder = enabled) }
        revalidate()
    }

    fun updateReminderTime(time: LocalTime) {
        _uiState.update { it.copy(reminderTime = time) }
        revalidate()
    }

    fun updateStartDate(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
        revalidate()
    }

    fun saveHabit(onComplete: (Long) -> Unit) {
        revalidate()
        if (!_uiState.value.isValid) return

        viewModelScope.launch {
            val candidate = buildCandidateHabit(_uiState.value)
            val original = _uiState.value.originalHabit

            val savedId = if (original != null && original.type != candidate.type) {
                // Type changed: apply atomic changeHabitType
                repository.changeHabitType(candidate.id, candidate.type, candidate.target, candidate.unit)
                repository.saveHabit(candidate)
                candidate.id
            } else {
                repository.saveHabit(candidate)
            }

            val finalHabit = candidate.copy(id = savedId)

            if (finalHabit.reminderTime != null) {
                onScheduleReminder(finalHabit)
            } else {
                onCancelReminder(finalHabit.id)
            }

            _uiState.update { it.copy(isSaved = true) }
            onComplete(savedId)
        }
    }
}
