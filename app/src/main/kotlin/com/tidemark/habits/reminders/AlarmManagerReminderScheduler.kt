package com.tidemark.habits.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tidemark.habits.domain.logic.ReminderPlanner
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.time.TimeProvider

class AlarmManagerReminderScheduler(
    private val context: Context,
    private val timeProvider: TimeProvider
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(habit: Habit) {
        if (habit.reminderTime == null) {
            cancelReminder(habit.id)
            return
        }

        val nextTrigger = ReminderPlanner.nextTrigger(
            habit = habit,
            after = timeProvider.now(),
            zone = timeProvider.zone()
        ) ?: return

        val triggerEpochMs = nextTrigger.toEpochMilli()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            data = Uri.parse("tidemark://habit/${habit.id}")
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habit.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (PermissionHelper.canScheduleExactAlarms(context)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMs,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMs,
                pendingIntent
            )
        }
    }

    fun cancelReminder(habitId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            data = Uri.parse("tidemark://habit/$habitId")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        NotificationHelper.cancelNotification(context, habitId)
    }

    fun rescheduleAll(habits: List<Habit>) {
        for (habit in habits) {
            if (habit.reminderTime != null) {
                scheduleReminder(habit)
            } else {
                cancelReminder(habit.id)
            }
        }
    }

    fun scheduleTestReminder(secondsFromNow: Long = 10L) {
        val triggerEpochMs = timeProvider.now().plusSeconds(secondsFromNow).toEpochMilli()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TEST_REMINDER
            putExtra(ReminderReceiver.EXTRA_IS_TEST, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (PermissionHelper.canScheduleExactAlarms(context)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMs,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMs,
                pendingIntent
            )
        }
    }
}
