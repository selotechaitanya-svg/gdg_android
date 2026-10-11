package com.tidemark.habits.domain.logic

import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitType

/**
 * Pure converter for adjusting existing habit logs when changing a habit's type.
 */
object HabitTypeConverter {
    /**
     * Converts a single [log] from [fromType] to [toType] with [newTarget].
     *
     * Rules:
     * - YES_NO -> COUNTABLE/TIMED: completed logs get value = newTarget, uncompleted get 0.
     * - COUNTABLE/TIMED -> YES_NO: value becomes 1 if completed else 0.
     * - COUNTABLE <-> TIMED: maintains existing values and completion flags.
     */
    fun convertLog(
        log: HabitLog,
        fromType: HabitType,
        toType: HabitType,
        newTarget: Int
    ): HabitLog {
        if (fromType == toType) return log

        return when {
            fromType == HabitType.YES_NO && (toType == HabitType.COUNTABLE || toType == HabitType.TIMED) -> {
                val newValue = if (log.completed) newTarget else 0
                val newCompleted = log.completed && newTarget > 0
                log.copy(value = newValue, completed = newCompleted)
            }
            (fromType == HabitType.COUNTABLE || fromType == HabitType.TIMED) && toType == HabitType.YES_NO -> {
                val newValue = if (log.completed) 1 else 0
                log.copy(value = newValue, completed = log.completed)
            }
            else -> {
                // COUNTABLE <-> TIMED
                log
            }
        }
    }

    /**
     * Converts all logs in [logs] from [fromType] to [toType].
     */
    fun convertLogs(
        logs: List<HabitLog>,
        fromType: HabitType,
        toType: HabitType,
        newTarget: Int
    ): List<HabitLog> {
        return logs.map { convertLog(it, fromType, toType, newTarget) }
    }
}
