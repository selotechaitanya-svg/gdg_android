package com.tidemark.habits.data

import com.tidemark.habits.data.settings.ActiveTimer
import com.tidemark.habits.data.settings.AppTheme
import com.tidemark.habits.data.settings.SettingsRepository
import com.tidemark.habits.data.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class FakeSettingsRepository : SettingsRepository(dataStore = null) {

    private val _themeFlow = MutableStateFlow(AppTheme.SYSTEM)
    override val themeFlow: Flow<AppTheme> = _themeFlow.asStateFlow()

    private val _activeTimerFlow = MutableStateFlow<ActiveTimer?>(null)
    override val activeTimerFlow: Flow<ActiveTimer?> = _activeTimerFlow.asStateFlow()

    override suspend fun setTheme(theme: AppTheme) {
        _themeFlow.value = theme
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeFlow.value = mode
    }

    override suspend fun startTimer(habitId: Long, startEpochMs: Long, startDate: LocalDate) {
        _activeTimerFlow.value = ActiveTimer(habitId, startEpochMs, startDate)
    }

    override suspend fun clearTimer() {
        _activeTimerFlow.value = null
    }
}
