package com.tidemark.habits.data.mapper

import com.tidemark.habits.data.db.entity.HabitLogEntity
import com.tidemark.habits.domain.model.HabitLog
import java.time.LocalDate

/**
 * Pure mappers converting between Room [HabitLogEntity] and domain [HabitLog].
 */
object LogMapper {
    /**
     * Converts a database [HabitLogEntity] to a domain [HabitLog].
     */
    fun toDomain(entity: HabitLogEntity): HabitLog {
        return HabitLog(
            habitId = entity.habitId,
            date = LocalDate.parse(entity.date),
            value = entity.value,
            completed = entity.completed
        )
    }

    /**
     * Converts a domain [HabitLog] to a persistent [HabitLogEntity].
     */
    fun toEntity(
        domain: HabitLog,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    ): HabitLogEntity {
        return HabitLogEntity(
            habitId = domain.habitId,
            date = domain.date.toString(),
            value = domain.value,
            completed = domain.completed,
            updatedAtEpochMs = updatedAtEpochMs
        )
    }
}
