package com.tidemark.habits.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing the persistent storage model for a habit.
 *
 * Schedule is encoded into [scheduleType], [weekdaysMask], and [timesPerWeek].
 * [weekdaysMask] stores bit 0 (Mon) through bit 6 (Sun).
 * [reminderMinutes] stores minutes after midnight (0..1439) or null if no reminder is armed.
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    val scheduleType: String,
    val weekdaysMask: Int,
    val timesPerWeek: Int,
    val target: Int,
    val unit: String?,
    val step: Int,
    val reminderMinutes: Int?,
    val startDate: String,
    val createdAtEpochMs: Long
)
