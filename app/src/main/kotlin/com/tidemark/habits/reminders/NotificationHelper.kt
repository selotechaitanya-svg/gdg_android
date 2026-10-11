package com.tidemark.habits.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tidemark.habits.MainActivity
import com.tidemark.habits.R
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitType
import java.time.LocalDate

object NotificationHelper {

    const val CHANNEL_ID = "habit_reminders"
    private const val CHANNEL_NAME = "Habit Reminders"
    private const val CHANNEL_DESC = "Scheduled daily notifications and progress reminders for your habits"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun postReminderNotification(
        context: Context,
        habit: Habit,
        currentProgress: Int,
        date: LocalDate
    ) {
        if (!PermissionHelper.canPostNotifications(context)) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Content intent: open app at today
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("tidemark://habit/${habit.id}")
            putExtra("EXTRA_DATE", date.toString())
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            habit.id.toInt(),
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // "Mark Done" Action intent (Bonus B2)
        val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_DONE
            putExtra(NotificationActionReceiver.EXTRA_HABIT_ID, habit.id)
            putExtra(NotificationActionReceiver.EXTRA_DATE, date.toString())
        }
        val actionPendingIntent = PendingIntent.getBroadcast(
            context,
            (habit.id * 1000L).toInt(),
            actionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val progressText = when (habit.type) {
            HabitType.YES_NO -> "Time for your daily routine!"
            HabitType.COUNTABLE -> "$currentProgress / ${habit.target} ${habit.unit ?: ""} logged today".trim()
            HabitType.TIMED -> "$currentProgress / ${habit.target} min logged today"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle(habit.name)
            .setContentText(progressText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_check,
                "Mark done",
                actionPendingIntent
            )
            .build()

        manager.notify(habit.id.toInt(), notification)
    }

    fun postTestNotification(context: Context) {
        if (!PermissionHelper.canPostNotifications(context)) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val contentIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle("Test Reminder")
            .setContentText("Tidemark notifications and exact alarms are active and working!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(9999, notification)
    }

    fun cancelNotification(context: Context, habitId: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(habitId.toInt())
    }
}
