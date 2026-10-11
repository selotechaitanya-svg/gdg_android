package com.tidemark.habits.data

import com.tidemark.habits.data.backup.BackupSerializer
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitLog
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupSerializerTest {

    private val today = LocalDate.of(2026, 10, 5)

    @Test
    fun `backup serializer round trip preserves all habits and logs`() {
        val habits = listOf(
            Habit(
                id = 1L,
                name = "Read",
                type = HabitType.COUNTABLE,
                schedule = Schedule.Daily,
                target = 20,
                unit = "pages",
                step = 5,
                reminderTime = LocalTime.of(21, 0),
                startDate = today,
                createdAt = Instant.parse("2026-10-05T00:00:00Z")
            ),
            Habit(
                id = 2L,
                name = "Gym",
                type = HabitType.YES_NO,
                schedule = Schedule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
                target = 1,
                unit = null,
                step = 1,
                reminderTime = null,
                startDate = today,
                createdAt = Instant.parse("2026-10-05T00:00:00Z")
            ),
            Habit(
                id = 3L,
                name = "Meditate",
                type = HabitType.TIMED,
                schedule = Schedule.TimesPerWeek(3),
                target = 15,
                unit = "min",
                step = 5,
                reminderTime = LocalTime.of(8, 0),
                startDate = today,
                createdAt = Instant.parse("2026-10-05T00:00:00Z")
            )
        )

        val logs = listOf(
            HabitLog(habitId = 1L, date = today, value = 20, completed = true),
            HabitLog(habitId = 2L, date = today, value = 1, completed = true),
            HabitLog(habitId = 3L, date = today, value = 15, completed = true)
        )

        val json = BackupSerializer.exportToJson(habits, logs)
        val parseResult = BackupSerializer.validateAndParse(json)

        assertTrue(parseResult.isSuccess)
        val (restoredHabits, restoredLogs) = parseResult.getOrThrow()

        assertEquals(3, restoredHabits.size)
        assertEquals(3, restoredLogs.size)
        assertEquals(habits[0].name, restoredHabits[0].name)
        assertEquals(habits[1].schedule, restoredHabits[1].schedule)
        assertEquals(habits[2].target, restoredHabits[2].target)
        assertEquals(logs[0].value, restoredLogs[0].value)
    }

    @Test
    fun `backup serializer rejects unsupported schema version`() {
        val json = """
            {
              "app": "tidemark",
              "schemaVersion": 2,
              "exportedAt": "2026-10-05T00:00:00Z",
              "habits": [],
              "logs": []
            }
        """.trimIndent()

        val result = BackupSerializer.validateAndParse(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unsupported backup schema version") == true)
    }

    @Test
    fun `backup serializer rejects invalid app identifier`() {
        val json = """
            {
              "app": "other_app",
              "schemaVersion": 1,
              "exportedAt": "2026-10-05T00:00:00Z",
              "habits": [],
              "logs": []
            }
        """.trimIndent()

        val result = BackupSerializer.validateAndParse(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid application identifier") == true)
    }

    @Test
    fun `backup serializer rejects referential integrity violation`() {
        val json = """
            {
              "app": "tidemark",
              "schemaVersion": 1,
              "exportedAt": "2026-10-05T00:00:00Z",
              "habits": [
                {
                  "id": 1,
                  "name": "Read",
                  "type": "COUNTABLE",
                  "scheduleType": "DAILY",
                  "target": 20,
                  "startDate": "2026-10-05",
                  "createdAt": "2026-10-05T00:00:00Z"
                }
              ],
              "logs": [
                {
                  "habitId": 999,
                  "date": "2026-10-05",
                  "value": 20,
                  "completed": true
                }
              ]
            }
        """.trimIndent()

        val result = BackupSerializer.validateAndParse(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Referential integrity violation") == true)
    }

    @Test
    fun `backup serializer rejects negative log values`() {
        val json = """
            {
              "app": "tidemark",
              "schemaVersion": 1,
              "exportedAt": "2026-10-05T00:00:00Z",
              "habits": [
                {
                  "id": 1,
                  "name": "Read",
                  "type": "COUNTABLE",
                  "scheduleType": "DAILY",
                  "target": 20,
                  "startDate": "2026-10-05",
                  "createdAt": "2026-10-05T00:00:00Z"
                }
              ],
              "logs": [
                {
                  "habitId": 1,
                  "date": "2026-10-05",
                  "value": -5,
                  "completed": false
                }
              ]
            }
        """.trimIndent()

        val result = BackupSerializer.validateAndParse(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot be negative") == true)
    }

    @Test
    fun `backup serializer rejects duplicate habit ids`() {
        val json = """
            {
              "app": "tidemark",
              "schemaVersion": 1,
              "exportedAt": "2026-10-05T00:00:00Z",
              "habits": [
                {
                  "id": 1,
                  "name": "Read",
                  "type": "COUNTABLE",
                  "scheduleType": "DAILY",
                  "target": 20,
                  "startDate": "2026-10-05",
                  "createdAt": "2026-10-05T00:00:00Z"
                },
                {
                  "id": 1,
                  "name": "Meditate",
                  "type": "YES_NO",
                  "scheduleType": "DAILY",
                  "target": 1,
                  "startDate": "2026-10-05",
                  "createdAt": "2026-10-05T00:00:00Z"
                }
              ],
              "logs": []
            }
        """.trimIndent()

        val result = BackupSerializer.validateAndParse(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Duplicate habit ID") == true)
    }

    @Test
    fun `backup serializer rejects duplicate logs for same habit and date`() {
        val json = """
            {
              "app": "tidemark",
              "schemaVersion": 1,
              "exportedAt": "2026-10-05T00:00:00Z",
              "habits": [
                {
                  "id": 1,
                  "name": "Read",
                  "type": "COUNTABLE",
                  "scheduleType": "DAILY",
                  "target": 20,
                  "startDate": "2026-10-05",
                  "createdAt": "2026-10-05T00:00:00Z"
                }
              ],
              "logs": [
                {
                  "habitId": 1,
                  "date": "2026-10-05",
                  "value": 10,
                  "completed": false
                },
                {
                  "habitId": 1,
                  "date": "2026-10-05",
                  "value": 20,
                  "completed": true
                }
              ]
            }
        """.trimIndent()

        val result = BackupSerializer.validateAndParse(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Duplicate log entry") == true)
    }
}
