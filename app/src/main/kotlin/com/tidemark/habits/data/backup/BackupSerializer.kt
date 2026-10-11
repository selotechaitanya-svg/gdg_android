package com.tidemark.habits.data.backup

import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

/**
 * Pure JSON serializer and validator for database backup export and import.
 * Ensures complete referential integrity, schema version check, and field bounds validation.
 */
object BackupSerializer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

    /**
     * Serializes [habits] and [logs] into an export JSON string.
     */
    fun exportToJson(habits: List<Habit>, logs: List<HabitLog>): String {
        val backupHabits = habits.map { habit ->
            val (schedType, weekdays, times) = when (val s = habit.schedule) {
                is Schedule.Daily -> Triple("DAILY", emptyList(), 0)
                is Schedule.Weekdays -> Triple("WEEKDAYS", s.days.map { it.name }, 0)
                is Schedule.TimesPerWeek -> Triple("TIMES_PER_WEEK", emptyList(), s.times)
            }
            BackupHabit(
                id = habit.id,
                name = habit.name,
                type = habit.type.name,
                scheduleType = schedType,
                weekdays = weekdays,
                timesPerWeek = times,
                target = habit.target,
                unit = habit.unit,
                step = habit.step,
                reminderTime = habit.reminderTime?.toString(),
                startDate = habit.startDate.toString(),
                createdAt = habit.createdAt.toString()
            )
        }

        val backupLogs = logs.map { log ->
            BackupHabitLog(
                habitId = log.habitId,
                date = log.date.toString(),
                value = log.value,
                completed = log.completed
            )
        }

        val envelope = BackupEnvelope(
            app = "tidemark",
            schemaVersion = 1,
            exportedAt = Instant.now().toString(),
            habits = backupHabits,
            logs = backupLogs
        )

        return json.encodeToString(BackupEnvelope.serializer(), envelope)
    }

    /**
     * Parses and strictly validates JSON string.
     * Returns Result.success with domain habits and logs, or Result.failure with a descriptive error.
     */
    fun validateAndParse(jsonString: String): Result<Pair<List<Habit>, List<HabitLog>>> {
        return runCatching {
            val envelope = try {
                json.decodeFromString(BackupEnvelope.serializer(), jsonString)
            } catch (e: Exception) {
                throw IllegalArgumentException("Malformed JSON format: ${e.message}", e)
            }

            if (envelope.app != "tidemark") {
                throw IllegalArgumentException("Invalid application identifier '${envelope.app}'. Expected 'tidemark'.")
            }

            if (envelope.schemaVersion != 1) {
                throw IllegalArgumentException("Unsupported backup schema version ${envelope.schemaVersion}. Expected version 1.")
            }

            try {
                Instant.parse(envelope.exportedAt)
            } catch (e: DateTimeParseException) {
                throw IllegalArgumentException("Invalid export timestamp: ${envelope.exportedAt}")
            }

            val habitIds = mutableSetOf<Long>()
            val domainHabits = envelope.habits.map { h ->
                if (h.id <= 0) {
                    throw IllegalArgumentException("Invalid habit ID ${h.id}")
                }
                if (h.name.trim().isEmpty() || h.name.length > 40) {
                    throw IllegalArgumentException("Habit '${h.name}' has invalid name length (must be 1..40)")
                }
                val type = try {
                    HabitType.valueOf(h.type)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid habit type '${h.type}' for habit '${h.name}'")
                }

                if (h.target < 1) {
                    throw IllegalArgumentException("Invalid target ${h.target} for habit '${h.name}'")
                }

                val schedule = when (h.scheduleType) {
                    "DAILY" -> Schedule.Daily
                    "WEEKDAYS" -> {
                        if (h.weekdays.isEmpty()) {
                            throw IllegalArgumentException("Weekdays schedule for '${h.name}' cannot be empty")
                        }
                        val days = h.weekdays.map { dayStr ->
                            try {
                                DayOfWeek.valueOf(dayStr)
                            } catch (e: Exception) {
                                throw IllegalArgumentException("Invalid weekday '$dayStr' for habit '${h.name}'")
                            }
                        }.toSet()
                        Schedule.Weekdays(days)
                    }
                    "TIMES_PER_WEEK" -> {
                        if (h.timesPerWeek !in 1..7) {
                            throw IllegalArgumentException("Times per week ${h.timesPerWeek} for '${h.name}' must be between 1 and 7")
                        }
                        Schedule.TimesPerWeek(h.timesPerWeek)
                    }
                    else -> throw IllegalArgumentException("Unknown schedule type '${h.scheduleType}' for habit '${h.name}'")
                }

                val reminder = h.reminderTime?.let {
                    try {
                        LocalTime.parse(it)
                    } catch (e: DateTimeParseException) {
                        throw IllegalArgumentException("Invalid reminder time '$it' for habit '${h.name}'")
                    }
                }

                val startDate = try {
                    LocalDate.parse(h.startDate)
                } catch (e: DateTimeParseException) {
                    throw IllegalArgumentException("Invalid start date '${h.startDate}' for habit '${h.name}'")
                }

                val createdAt = try {
                    Instant.parse(h.createdAt)
                } catch (e: DateTimeParseException) {
                    throw IllegalArgumentException("Invalid createdAt '${h.createdAt}' for habit '${h.name}'")
                }

                habitIds.add(h.id)
                Habit(
                    id = h.id,
                    name = h.name,
                    type = type,
                    schedule = schedule,
                    target = h.target,
                    unit = h.unit,
                    step = maxOf(1, h.step),
                    reminderTime = reminder,
                    startDate = startDate,
                    createdAt = createdAt
                )
            }

            val domainLogs = envelope.logs.map { l ->
                // Referential integrity check
                if (!habitIds.contains(l.habitId)) {
                    throw IllegalArgumentException("Referential integrity violation: log references non-existent habit ID ${l.habitId}")
                }

                val date = try {
                    LocalDate.parse(l.date)
                } catch (e: DateTimeParseException) {
                    throw IllegalArgumentException("Invalid log date '${l.date}' for habit ID ${l.habitId}")
                }

                if (l.value < 0) {
                    throw IllegalArgumentException("Log value cannot be negative: ${l.value}")
                }

                HabitLog(
                    habitId = l.habitId,
                    date = date,
                    value = l.value,
                    completed = l.completed
                )
            }

            Pair(domainHabits, domainLogs)
        }
    }
}
