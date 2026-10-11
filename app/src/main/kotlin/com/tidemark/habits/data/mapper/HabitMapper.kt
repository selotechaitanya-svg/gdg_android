package com.tidemark.habits.data.mapper

import com.tidemark.habits.data.db.entity.HabitEntity
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pure mappers converting between Room [HabitEntity] and domain [Habit].
 */
object HabitMapper {

    /**
     * Converts a database [HabitEntity] to a domain [Habit].
     */
    fun toDomain(entity: HabitEntity): Habit {
        val type = HabitType.valueOf(entity.type)
        val schedule = when (entity.scheduleType) {
            "DAILY" -> Schedule.Daily
            "WEEKDAYS" -> Schedule.Weekdays(maskToDays(entity.weekdaysMask))
            "TIMES_PER_WEEK" -> Schedule.TimesPerWeek(entity.timesPerWeek)
            else -> Schedule.Daily
        }
        val reminderTime = entity.reminderMinutes?.let { minutes ->
            LocalTime.of(minutes / 60, minutes % 60)
        }
        val startDate = LocalDate.parse(entity.startDate)
        val createdAt = Instant.ofEpochMilli(entity.createdAtEpochMs)

        return Habit(
            id = entity.id,
            name = entity.name,
            type = type,
            schedule = schedule,
            target = entity.target,
            unit = entity.unit,
            step = entity.step,
            reminderTime = reminderTime,
            startDate = startDate,
            createdAt = createdAt
        )
    }

    /**
     * Converts a domain [Habit] to a persistent [HabitEntity].
     */
    fun toEntity(domain: Habit): HabitEntity {
        val (scheduleType, mask, times) = when (val s = domain.schedule) {
            is Schedule.Daily -> Triple("DAILY", 0, 0)
            is Schedule.Weekdays -> Triple("WEEKDAYS", daysToMask(s.days), 0)
            is Schedule.TimesPerWeek -> Triple("TIMES_PER_WEEK", 0, s.times)
        }
        val reminderMinutes = domain.reminderTime?.let { it.hour * 60 + it.minute }

        return HabitEntity(
            id = domain.id,
            name = domain.name,
            type = domain.type.name,
            scheduleType = scheduleType,
            weekdaysMask = mask,
            timesPerWeek = times,
            target = domain.target,
            unit = domain.unit,
            step = domain.step,
            reminderMinutes = reminderMinutes,
            startDate = domain.startDate.toString(),
            createdAtEpochMs = domain.createdAt.toEpochMilli()
        )
    }

    /**
     * Converts a set of [DayOfWeek] to a compact 7-bit integer mask.
     * Monday = bit 0 (1), Sunday = bit 6 (64).
     */
    fun daysToMask(days: Set<DayOfWeek>): Int {
        return days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }
    }

    /**
     * Unpacks a 7-bit integer mask back into a Set of [DayOfWeek].
     */
    fun maskToDays(mask: Int): Set<DayOfWeek> {
        return DayOfWeek.values().filter { day ->
            (mask and (1 shl (day.value - 1))) != 0
        }.toSet()
    }
}
