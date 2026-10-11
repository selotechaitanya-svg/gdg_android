package com.tidemark.habits.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tidemark.habits.data.db.dao.HabitDao
import com.tidemark.habits.data.db.dao.HabitLogDao
import com.tidemark.habits.data.db.entity.HabitEntity
import com.tidemark.habits.data.db.entity.HabitLogEntity

/**
 * Main Room database definition for Tidemark.
 * Version 1 with explicit schema export enabled.
 */
@Database(
    entities = [HabitEntity::class, HabitLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
}
