package com.tidemark.habits.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tidemark.habits.data.backup.BackupSerializer
import com.tidemark.habits.data.settings.SettingsRepository
import com.tidemark.habits.data.settings.ThemeMode
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import com.tidemark.habits.domain.repo.HabitRepository
import com.tidemark.habits.domain.time.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val importMessage: String? = null,
    val isImportSuccess: Boolean = false,
    val testReminderScheduledMessage: String? = null
)

class SettingsViewModel(
    private val repository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val onScheduleTestReminder: (secondsFromNow: Long) -> Unit,
    private val onRescheduleAll: () -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.themeModeFlow.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun triggerTestReminder() {
        onScheduleTestReminder(10L)
        _uiState.update {
            it.copy(testReminderScheduledMessage = "Test reminder armed! Will fire in 10 seconds.")
        }
    }

    fun clearTestReminderMessage() {
        _uiState.update { it.copy(testReminderScheduledMessage = null) }
    }

    suspend fun exportDataJson(): String {
        val habits = repository.observeHabits().first()
        val allLogs = mutableListOf<HabitLog>()
        for (habit in habits) {
            allLogs.addAll(repository.getLogs(habit.id))
        }
        return BackupSerializer.exportToJson(habits, allLogs)
    }

    fun importDataJson(jsonString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importMessage = null) }
            val parseResult = BackupSerializer.validateAndParse(jsonString)
            parseResult.fold(
                onSuccess = { (habits, logs) ->
                    repository.replaceAll(habits, logs)
                    onRescheduleAll()
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            isImportSuccess = true,
                            importMessage = "Successfully restored ${habits.size} habits and ${logs.size} logs."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            isImportSuccess = false,
                            importMessage = "Import failed: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            val today = timeProvider.today()
            val start = today.minusDays(44)
            val now = timeProvider.now()

            val habit1 = Habit(
                id = 1,
                name = "Read",
                type = HabitType.COUNTABLE,
                schedule = Schedule.Daily,
                target = 20,
                unit = "pages",
                step = 5,
                reminderTime = LocalTime.of(21, 0),
                startDate = start,
                createdAt = now
            )
            val habit2 = Habit(
                id = 2,
                name = "Gym",
                type = HabitType.YES_NO,
                schedule = Schedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
                target = 1,
                unit = null,
                step = 1,
                reminderTime = LocalTime.of(7, 30),
                startDate = start,
                createdAt = now
            )
            val habit3 = Habit(
                id = 3,
                name = "Drink water",
                type = HabitType.COUNTABLE,
                schedule = Schedule.Daily,
                target = 8,
                unit = "glasses",
                step = 1,
                reminderTime = LocalTime.of(14, 0),
                startDate = start,
                createdAt = now
            )
            val habit4 = Habit(
                id = 4,
                name = "Meditate",
                type = HabitType.TIMED,
                schedule = Schedule.TimesPerWeek(3),
                target = 15,
                unit = "min",
                step = 5,
                reminderTime = LocalTime.of(22, 0),
                startDate = start,
                createdAt = now
            )

            val demoHabits = listOf(habit1, habit2, habit3, habit4)
            val demoLogs = mutableListOf<HabitLog>()

            val rng = Random(42)
            var curr = start
            while (!curr.isAfter(today)) {
                // Read: high consistency with a couple of realistic misses
                val readPages = if (rng.nextDouble() < 0.85) (20..35).random(rng) else (8..15).random(rng)
                demoLogs.add(HabitLog(1, curr, readPages, completed = readPages >= 20))

                // Gym: Mon/Wed/Fri
                if (curr.dayOfWeek == DayOfWeek.MONDAY || curr.dayOfWeek == DayOfWeek.WEDNESDAY || curr.dayOfWeek == DayOfWeek.FRIDAY) {
                    val gymDone = rng.nextDouble() < 0.88
                    if (gymDone) {
                        demoLogs.add(HabitLog(2, curr, 1, completed = true))
                    }
                }

                // Water: 6-10 glasses
                val water = (6..10).random(rng)
                demoLogs.add(HabitLog(3, curr, water, completed = water >= 8))

                // Meditate: 3x per week
                if (rng.nextDouble() < 0.45) {
                    demoLogs.add(HabitLog(4, curr, 15, completed = true))
                }

                curr = curr.plusDays(1)
            }

            repository.replaceAll(demoHabits, demoLogs)
            onRescheduleAll()

            _uiState.update {
                it.copy(
                    isImportSuccess = true,
                    importMessage = "Loaded demo data: 4 habits, ${demoLogs.size} logs."
                )
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.replaceAll(emptyList(), emptyList())
            onRescheduleAll()
            _uiState.update {
                it.copy(
                    isImportSuccess = true,
                    importMessage = "All data cleared successfully."
                )
            }
        }
    }

    fun dismissImportMessage() {
        _uiState.update { it.copy(importMessage = null) }
    }
}
