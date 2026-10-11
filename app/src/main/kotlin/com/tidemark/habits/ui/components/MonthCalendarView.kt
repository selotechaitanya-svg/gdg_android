package com.tidemark.habits.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tidemark.habits.domain.model.DayInfo
import com.tidemark.habits.domain.model.DayState
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.ui.theme.LocalTidemarkColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Custom Monday-first month calendar grid displaying DayState visuals and allowing past day corrections.
 */
@Composable
fun MonthCalendarView(
    habit: Habit,
    dayInfos: List<DayInfo>,
    selectedMonth: YearMonth,
    today: LocalDate,
    onMonthChanged: (YearMonth) -> Unit,
    onDayClicked: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTidemarkColors.current
    val primary = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        // Month Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onMonthChanged(selectedMonth.minusMonths(1)) },
                modifier = Modifier.size(44.dp)
            ) {
                Text("<", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }

            Text(
                text = "${selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${selectedMonth.year}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            IconButton(
                onClick = { onMonthChanged(selectedMonth.plusMonths(1)) },
                modifier = Modifier.size(44.dp)
            ) {
                Text(">", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekday column headers (Mon..Sun)
        Row(modifier = Modifier.fillMaxWidth()) {
            val weekdays = listOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
            )
            for (w in weekdays) {
                Text(
                    text = w.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(1),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid calculation
        val firstDayOfMonth = selectedMonth.atDay(1)
        val firstDayOffset = firstDayOfMonth.dayOfWeek.value - 1 // Mon = 0
        val daysInMonth = selectedMonth.lengthOfMonth()
        val totalCells = ((firstDayOffset + daysInMonth + 6) / 7) * 7

        val dayInfoMap = remember(dayInfos) { dayInfos.associateBy { it.date } }

        Column {
            for (row in 0 until (totalCells / 7)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOffset + 1

                        if (dayNumber in 1..daysInMonth) {
                            val cellDate = selectedMonth.atDay(dayNumber)
                            val dayInfo = dayInfoMap[cellDate]
                            val state = dayInfo?.state ?: DayState.FUTURE
                            val isToday = cellDate == today
                            val isClickable = !cellDate.isBefore(habit.startDate) && !cellDate.isAfter(today)

                            val backgroundColor = when (state) {
                                DayState.COMPLETE -> primary
                                DayState.PARTIAL -> colors.calendarPartial
                                DayState.MISSED -> colors.calendarMissed
                                else -> Color.Transparent
                            }

                            val textColor = when (state) {
                                DayState.COMPLETE -> MaterialTheme.colorScheme.onPrimary
                                DayState.MISSED -> MaterialTheme.colorScheme.onSurface
                                DayState.BEFORE_START, DayState.FUTURE -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            val borderModifier = when {
                                isToday -> Modifier.border(2.dp, primary, CircleShape)
                                state == DayState.UNSCHEDULED -> Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                else -> Modifier
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(backgroundColor)
                                    .then(borderModifier)
                                    .clickable(enabled = isClickable) {
                                        onDayClicked(cellDate)
                                    }
                                    .semantics {
                                        contentDescription = "$cellDate, status $state"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$dayNumber",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = textColor,
                                        fontWeight = if (isToday || state == DayState.COMPLETE) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        } else {
                            // Blank cell padding outside month range
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = primary, label = "Complete")
            LegendItem(color = colors.calendarPartial, label = "Partial")
            LegendItem(color = colors.calendarMissed, label = "Missed")
            LegendItem(color = Color.Transparent, borderColor = MaterialTheme.colorScheme.outline, label = "Unscheduled")
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    borderColor: Color? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val borderMod = if (borderColor != null) Modifier.border(1.dp, borderColor, CircleShape) else Modifier
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
                .then(borderMod)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
