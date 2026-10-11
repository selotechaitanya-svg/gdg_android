package com.tidemark.habits.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tidemark.habits.data.db.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the habit_logs table.
 */
@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date ASC")
    fun observeByHabitId(habitId: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeBetween(from: String, to: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs ORDER BY date ASC")
    fun observeAll(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun getByHabitAndDate(habitId: Long, date: String): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date ASC")
    suspend fun getAllByHabitId(habitId: Long): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAll(): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: HabitLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(logs: List<HabitLogEntity>)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: Long, date: String)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteByHabitId(habitId: Long)

    @Query("DELETE FROM habit_logs")
    suspend fun deleteAll()
}
