package com.tidemark.habits.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tidemark.habits.TidemarkApp
import com.tidemark.habits.domain.logic.ReminderPlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "com.tidemark.habits.ACTION_REMINDER"
        const val ACTION_TEST_REMINDER = "com.tidemark.habits.ACTION_TEST_REMINDER"
        const val EXTRA_HABIT_ID = "EXTRA_HABIT_ID"
        const val EXTRA_IS_TEST = "EXTRA_IS_TEST"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isTest) {
                    NotificationHelper.postTestNotification(context)
                    return@launch
                }

                if (habitId <= 0) return@launch

                val app = context.applicationContext as? TidemarkApp ?: return@launch
                val container = app.container

                val habit = container.habitRepository.getHabit(habitId) ?: return@launch
                if (habit.reminderTime == null) return@launch

                val today = container.timeProvider.today()
                val logs = container.habitRepository.getLogs(habitId)

                // Check whether to notify using pure domain logic
                if (ReminderPlanner.shouldNotify(habit, logs, today)) {
                    val todayLog = logs.find { it.date == today }
                    val currentProgress = todayLog?.value ?: 0
                    NotificationHelper.postReminderNotification(
                        context = context,
                        habit = habit,
                        currentProgress = currentProgress,
                        date = today
                    )
                }

                // ALWAYS arm the next trigger occurrence
                container.reminderScheduler.scheduleReminder(habit)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
