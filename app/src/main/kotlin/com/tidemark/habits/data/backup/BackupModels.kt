package com.tidemark.habits.data.backup

import kotlinx.serialization.Serializable

/**
 * Top-level JSON envelope for data export and import.
 */
@Serializable
data class BackupEnvelope(
    val app: String,
    val schemaVersion: Int,
    val exportedAt: String,
    val habits: List<BackupHabit>,
    val logs: List<BackupHabitLog>
)

/**
 * Portable representation of a habit for JSON backup.
 */
@Serializable
data class BackupHabit(
    val id: Long,
    val name: String,
    val type: String,
    val scheduleType: String,
    val weekdays: List<String> = emptyList(),
    val timesPerWeek: Int = 0,
    val target: Int,
    val unit: String? = null,
    val step: Int = 1,
    val reminderTime: String? = null,
    val startDate: String,
    val createdAt: String
)

/**
 * Portable representation of a habit progress log for JSON backup.
 */
@Serializable
data class BackupHabitLog(
    val habitId: Long,
    val date: String,
    val value: Int,
    val completed: Boolean
)
