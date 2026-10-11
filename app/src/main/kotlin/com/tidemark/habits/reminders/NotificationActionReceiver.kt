package com.tidemark.habits.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tidemark.habits.TidemarkApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Handles the "Mark Done" action button directly from a reminder notification.
 * Credits the specific date for which the reminder was issued (even if tapped after midnight),
 * sets value = max(existing, target) and completed = true, then cancels the notification.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_DONE = "com.tidemark.habits.ACTION_MARK_DONE"
        const val EXTRA_HABIT_ID = "EXTRA_HABIT_ID"
        const val EXTRA_DATE = "EXTRA_DATE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_DONE) return

        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val dateString = intent.getStringExtra(EXTRA_DATE) ?: return

        if (habitId <= 0) return

        val targetDate = try {
            LocalDate.parse(dateString)
        } catch (e: Exception) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? TidemarkApp ?: return@launch
                val container = app.container

                val habit = container.habitRepository.getHabit(habitId) ?: return@launch
                val currentLog = container.habitRepository.getLog(habitId, targetDate)
                val existingVal = currentLog?.value ?: 0
                val finalVal = maxOf(existingVal, habit.target)

                // Mark completed in database
                container.habitRepository.setLog(
                    habitId = habitId,
                    date = targetDate,
                    value = finalVal,
                    completed = true
                )

                // Cancel the notification
                NotificationHelper.cancelNotification(context, habitId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
