package com.tidemark.habits.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * User-selected theme mode.
 */
enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

typealias ThemeMode = AppTheme

/**
 * Persisted stopwatch state for timed habits that survives process death and screen rotation.
 */
data class ActiveTimer(
    val habitId: Long,
    val startEpochMs: Long,
    val startDate: LocalDate
)

/**
 * DataStore-backed repository managing application settings, theme preferences,
 * and active stopwatch sessions.
 */
open class SettingsRepository(
    private val dataStore: DataStore<Preferences>? = null
) {
    private object Keys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val TIMER_HABIT_ID = longPreferencesKey("timer_habit_id")
        val TIMER_START_EPOCH_MS = longPreferencesKey("timer_start_epoch_ms")
        val TIMER_START_DATE = stringPreferencesKey("timer_start_date")
    }

    open val themeFlow: Flow<AppTheme> = dataStore?.data?.map { prefs ->
        val name = prefs[Keys.APP_THEME] ?: AppTheme.SYSTEM.name
        try {
            AppTheme.valueOf(name)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        }
    } ?: flowOf(AppTheme.SYSTEM)

    open val themeModeFlow: Flow<ThemeMode> get() = themeFlow

    open val activeTimerFlow: Flow<ActiveTimer?> = dataStore?.data?.map { prefs ->
        val habitId = prefs[Keys.TIMER_HABIT_ID]
        val startEpoch = prefs[Keys.TIMER_START_EPOCH_MS]
        val startDateStr = prefs[Keys.TIMER_START_DATE]

        if (habitId != null && startEpoch != null && startDateStr != null) {
            try {
                ActiveTimer(
                    habitId = habitId,
                    startEpochMs = startEpoch,
                    startDate = LocalDate.parse(startDateStr)
                )
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    } ?: flowOf(null)

    open suspend fun setTheme(theme: AppTheme) {
        dataStore?.edit { prefs ->
            prefs[Keys.APP_THEME] = theme.name
        }
    }

    open suspend fun setThemeMode(mode: ThemeMode) = setTheme(mode)

    open suspend fun startTimer(habitId: Long, startEpochMs: Long, startDate: LocalDate) {
        dataStore?.edit { prefs ->
            prefs[Keys.TIMER_HABIT_ID] = habitId
            prefs[Keys.TIMER_START_EPOCH_MS] = startEpochMs
            prefs[Keys.TIMER_START_DATE] = startDate.toString()
        }
    }

    open suspend fun clearTimer() {
        dataStore?.edit { prefs ->
            prefs.remove(Keys.TIMER_HABIT_ID)
            prefs.remove(Keys.TIMER_START_EPOCH_MS)
            prefs.remove(Keys.TIMER_START_DATE)
        }
    }
}
