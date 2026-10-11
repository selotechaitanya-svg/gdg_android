package com.tidemark.habits.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity representing an individual habit progress record on a given calendar date.
 * Enforces cascade deletion when the parent habit is deleted.
 * Composite primary key: ([habitId], [date]) guarantees at most one record per habit per day.
 */
@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("date")
    ]
)
data class HabitLogEntity(
    val habitId: Long,
    val date: String,
    val value: Int,
    val completed: Boolean,
    val updatedAtEpochMs: Long
)
