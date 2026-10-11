package com.tidemark.habits.data

import com.tidemark.habits.domain.logic.HabitTypeConverter
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.repo.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeHabitRepository : HabitRepository {

    private val habitsFlow = MutableStateFlow<List<Habit>>(emptyList())
    private val logsFlow = MutableStateFlow<List<HabitLog>>(emptyList())

    override fun observeHabits(): Flow<List<Habit>> = habitsFlow.asStateFlow()

    override fun observeHabit(id: Long): Flow<Habit?> {
        return habitsFlow.map { list -> list.find { it.id == id } }
    }

    override fun observeLogs(habitId: Long): Flow<List<HabitLog>> {
        return logsFlow.map { list -> list.filter { it.habitId == habitId } }
    }

    override fun observeLogsBetween(from: LocalDate, to: LocalDate): Flow<List<HabitLog>> {
        return logsFlow.map { list ->
            list.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
        }
    }

    override suspend fun getHabit(id: Long): Habit? {
        return habitsFlow.value.find { it.id == id }
    }

    override suspend fun getLog(habitId: Long, date: LocalDate): HabitLog? {
        return logsFlow.value.find { it.habitId == habitId && it.date == date }
    }

    override suspend fun getLogs(habitId: Long): List<HabitLog> {
        return logsFlow.value.filter { it.habitId == habitId }
    }

    override suspend fun getHabits(): List<Habit> {
        return habitsFlow.value
    }

    override suspend fun saveHabit(habit: Habit): Long {
        val current = habitsFlow.value.toMutableList()
        val assignedId = if (habit.id == 0L) {
            (current.maxOfOrNull { it.id } ?: 0L) + 1L
        } else {
            habit.id
        }
        val newHabit = habit.copy(id = assignedId)
        val index = current.indexOfFirst { it.id == assignedId }
        if (index >= 0) {
            current[index] = newHabit
        } else {
            current.add(newHabit)
        }
        habitsFlow.value = current
        return assignedId
    }

    override suspend fun deleteHabit(id: Long) {
        habitsFlow.value = habitsFlow.value.filter { it.id != id }
        logsFlow.value = logsFlow.value.filter { it.habitId != id }
    }

    override suspend fun setLog(habitId: Long, date: LocalDate, value: Int, completed: Boolean) {
        val current = logsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.habitId == habitId && it.date == date }
        val newLog = HabitLog(habitId = habitId, date = date, value = value, completed = completed)
        if (index >= 0) {
            current[index] = newLog
        } else {
            current.add(newLog)
        }
        logsFlow.value = current
    }

    override suspend fun clearLog(habitId: Long, date: LocalDate) {
        logsFlow.value = logsFlow.value.filterNot { it.habitId == habitId && it.date == date }
    }

    override suspend fun changeHabitType(
        habitId: Long,
        newType: HabitType,
        newTarget: Int,
        newUnit: String?
    ) {
        val habit = getHabit(habitId) ?: return
        val updatedHabit = habit.copy(type = newType, target = newTarget, unit = newUnit)
        saveHabit(updatedHabit)

        val currentLogs = getLogs(habitId)
        val converted = HabitTypeConverter.convertLogs(currentLogs, habit.type, newType, newTarget)
        val otherLogs = logsFlow.value.filter { it.habitId != habitId }
        logsFlow.value = otherLogs + converted
    }

    override suspend fun replaceAll(habits: List<Habit>, logs: List<HabitLog>) {
        habitsFlow.value = habits
        logsFlow.value = logs
    }
}
