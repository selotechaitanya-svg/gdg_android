package com.tidemark.habits.data

import androidx.room.withTransaction
import com.tidemark.habits.data.db.AppDatabase
import com.tidemark.habits.data.db.dao.HabitDao
import com.tidemark.habits.data.db.dao.HabitLogDao
import com.tidemark.habits.data.db.entity.HabitLogEntity
import com.tidemark.habits.data.mapper.HabitMapper
import com.tidemark.habits.data.mapper.LogMapper
import com.tidemark.habits.domain.logic.HabitTypeConverter
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.repo.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Production implementation of [HabitRepository] using Room persistence.
 * Uses atomic transactions for all multi-step modifications and exposes reactive Flows for UI observation.
 */
class RoomHabitRepository(
    private val habitDao: HabitDao,
    private val logDao: HabitLogDao,
    private val database: AppDatabase
) : HabitRepository {

    constructor(database: AppDatabase) : this(
        habitDao = database.habitDao(),
        logDao = database.habitLogDao(),
        database = database
    )

    override fun observeHabits(): Flow<List<Habit>> {
        return habitDao.observeAll().map { entities ->
            entities.map { HabitMapper.toDomain(it) }
        }
    }

    override fun observeHabit(id: Long): Flow<Habit?> {
        return habitDao.observeById(id).map { entity ->
            entity?.let { HabitMapper.toDomain(it) }
        }
    }

    override fun observeLogs(habitId: Long): Flow<List<HabitLog>> {
        return logDao.observeByHabitId(habitId).map { entities ->
            entities.map { LogMapper.toDomain(it) }
        }
    }

    override fun observeLogsBetween(from: LocalDate, to: LocalDate): Flow<List<HabitLog>> {
        return logDao.observeBetween(from.toString(), to.toString()).map { entities ->
            entities.map { LogMapper.toDomain(it) }
        }
    }

    override suspend fun getHabit(id: Long): Habit? {
        return habitDao.getById(id)?.let { HabitMapper.toDomain(it) }
    }

    override suspend fun getLog(habitId: Long, date: LocalDate): HabitLog? {
        return logDao.getByHabitAndDate(habitId, date.toString())?.let { LogMapper.toDomain(it) }
    }

    override suspend fun getLogs(habitId: Long): List<HabitLog> {
        return logDao.getAllByHabitId(habitId).map { LogMapper.toDomain(it) }
    }

    override suspend fun getHabits(): List<Habit> {
        return habitDao.getAll().map { HabitMapper.toDomain(it) }
    }

    override suspend fun saveHabit(habit: Habit): Long {
        val entity = HabitMapper.toEntity(habit)
        return if (habit.id == 0L) {
            habitDao.insert(entity)
        } else {
            habitDao.update(entity)
            habit.id
        }
    }

    override suspend fun deleteHabit(id: Long) {
        habitDao.deleteById(id)
    }

    override suspend fun setLog(habitId: Long, date: LocalDate, value: Int, completed: Boolean) {
        val entity = HabitLogEntity(
            habitId = habitId,
            date = date.toString(),
            value = value,
            completed = completed,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        logDao.upsert(entity)
    }

    override suspend fun clearLog(habitId: Long, date: LocalDate) {
        logDao.delete(habitId, date.toString())
    }

    override suspend fun changeHabitType(
        habitId: Long,
        newType: HabitType,
        newTarget: Int,
        newUnit: String?
    ) {
        database.withTransaction {
            val existingEntity = habitDao.getById(habitId) ?: return@withTransaction
            val existingDomain = HabitMapper.toDomain(existingEntity)
            val oldLogs = logDao.getAllByHabitId(habitId).map { LogMapper.toDomain(it) }

            val convertedLogs = HabitTypeConverter.convertLogs(
                logs = oldLogs,
                fromType = existingDomain.type,
                toType = newType,
                newTarget = newTarget
            )

            val updatedEntity = existingEntity.copy(
                type = newType.name,
                target = newTarget,
                unit = newUnit
            )
            habitDao.update(updatedEntity)

            val logEntities = convertedLogs.map { LogMapper.toEntity(it) }
            logDao.upsertAll(logEntities)
        }
    }

    override suspend fun replaceAll(habits: List<Habit>, logs: List<HabitLog>) {
        database.withTransaction {
            logDao.deleteAll()
            habitDao.deleteAll()

            for (habit in habits) {
                habitDao.insert(HabitMapper.toEntity(habit))
            }
            val logEntities = logs.map { LogMapper.toEntity(it) }
            logDao.upsertAll(logEntities)
        }
    }
}
