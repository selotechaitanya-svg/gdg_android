package com.tidemark.habits.domain.repo

import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Pure Kotlin repository interface for managing habits and habit progress logs.
 * Read operations expose reactive [Flow]s so UI and statistics update immediately.
 * Write operations are suspended and execute atomically.
 */
interface HabitRepository {
    /** Observe all habits ordered by creation date. */
    fun observeHabits(): Flow<List<Habit>>

    /** Observe a specific habit by its [id]. */
    fun observeHabit(id: Long): Flow<Habit?>

    /** Observe all logged days for a specific habit. */
    fun observeLogs(habitId: Long): Flow<List<HabitLog>>

    /** Observe logs across all habits between [from] and [to] inclusive. */
    fun observeLogsBetween(from: LocalDate, to: LocalDate): Flow<List<HabitLog>>

    /** Observe all logged days across all habits. */
    fun observeAllLogs(): Flow<List<HabitLog>>

    /** Fetch a single habit snapshot by [id]. */
    suspend fun getHabit(id: Long): Habit?

    /** Fetch a single log snapshot for [habitId] on [date]. */
    suspend fun getLog(habitId: Long, date: LocalDate): HabitLog?

    /** Fetch all logs for a given habit. */
    suspend fun getLogs(habitId: Long): List<HabitLog>

    /** Fetch all habits snapshot. */
    suspend fun getHabits(): List<Habit>

    /** Insert or update a habit. Returns the assigned habit ID. */
    suspend fun saveHabit(habit: Habit): Long

    /** Delete a habit and its associated logs. */
    suspend fun deleteHabit(id: Long)

    /** Record or update progress for a habit on a specific date. */
    suspend fun setLog(habitId: Long, date: LocalDate, value: Int, completed: Boolean)

    /** Clear / delete progress for a habit on a specific date. */
    suspend fun clearLog(habitId: Long, date: LocalDate)

    /** Change habit type and convert existing logs atomically. */
    suspend fun changeHabitType(habitId: Long, newType: HabitType, newTarget: Int, newUnit: String?)

    /** Replace the entire database state with backup data in a single transaction. */
    suspend fun replaceAll(habits: List<Habit>, logs: List<HabitLog>)
}
