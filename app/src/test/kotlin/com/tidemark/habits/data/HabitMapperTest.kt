package com.tidemark.habits.data

import com.tidemark.habits.data.mapper.HabitMapper
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals

class HabitMapperTest {

    @Test
    fun `bitmask encoding and decoding round trips correctly`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val mask = HabitMapper.daysToMask(days)
        // Mon = 1, Wed = 4, Fri = 16 => 21
        assertEquals(21, mask)

        val restoredDays = HabitMapper.maskToDays(mask)
        assertEquals(days, restoredDays)
    }

    @Test
    fun `habit entity mapping round trip preserves all fields`() {
        val habit = Habit(
            id = 42L,
            name = "Read Books",
            type = HabitType.COUNTABLE,
            schedule = Schedule.Weekdays(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)),
            target = 30,
            unit = "pages",
            step = 5,
            reminderTime = LocalTime.of(20, 45),
            startDate = LocalDate.of(2026, 10, 5),
            createdAt = Instant.parse("2026-10-05T08:30:00Z")
        )

        val entity = HabitMapper.toEntity(habit)
        assertEquals(42L, entity.id)
        assertEquals("Read Books", entity.name)
        assertEquals("COUNTABLE", entity.type)
        assertEquals("WEEKDAYS", entity.scheduleType)
        assertEquals(1245, entity.reminderMinutes) // 20 * 60 + 45 = 1245

        val restored = HabitMapper.toDomain(entity)
        assertEquals(habit, restored)
    }
}
