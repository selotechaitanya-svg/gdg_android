package com.tidemark.habits

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.tidemark.habits.data.RoomHabitRepository
import com.tidemark.habits.data.db.AppDatabase
import com.tidemark.habits.data.settings.SettingsRepository
import com.tidemark.habits.domain.repo.HabitRepository
import com.tidemark.habits.domain.time.SystemTimeProvider
import com.tidemark.habits.domain.time.TimeProvider
import com.tidemark.habits.reminders.AlarmManagerReminderScheduler
import com.tidemark.habits.reminders.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "tidemark_settings")

/**
 * Dependency container providing pure constructor-injected dependencies throughout the app.
 * Avoids reflection-based DI frameworks (Hilt/Koin) per architecture specifications.
 */
class AppContainer(val context: Context) {
    val timeProvider: TimeProvider = SystemTimeProvider()
    val database: AppDatabase = AppDatabase.getInstance(context)
    val habitRepository: HabitRepository = RoomHabitRepository(
        habitDao = database.habitDao(),
        habitLogDao = database.habitLogDao(),
        database = database
    )
    val settingsRepository: SettingsRepository = SettingsRepository(context.dataStore)
    val reminderScheduler: AlarmManagerReminderScheduler = AlarmManagerReminderScheduler(
        context = context,
        timeProvider = timeProvider
    )
}

class TidemarkApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Initialize reminder notification channel
        NotificationHelper.createNotificationChannel(this)

        // Idempotent startup reschedule of all active reminders
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habits = container.habitRepository.observeHabits().first()
                container.reminderScheduler.rescheduleAll(habits)
            } catch (e: Exception) {
                // Ignore transient db errors during boot
            }
        }
    }
}
