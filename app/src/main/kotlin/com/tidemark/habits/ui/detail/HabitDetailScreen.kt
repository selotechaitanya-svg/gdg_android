package com.tidemark.habits.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidemark.habits.R
import com.tidemark.habits.domain.model.DayInfo
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import com.tidemark.habits.ui.components.MonthCalendarView
import com.tidemark.habits.ui.components.WeeklyBarChart
import com.tidemark.habits.ui.theme.LocalTidemarkColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    viewModel: HabitDetailViewModel,
    today: LocalDate,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val habit = uiState.habit

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = habit?.name ?: "Habit Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    if (habit != null) {
                        IconButton(onClick = { onNavigateToEdit(habit.id) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = "Edit habit",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = "Delete habit",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (habit == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Habit not found")
            }
        } else {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Big Current Streak Hero Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_flame),
                                contentDescription = null,
                                tint = LocalTidemarkColors.current.coral,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${uiState.streak?.currentStreak ?: 0} ${uiState.streak?.unitName ?: "days"}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Current Streak",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.streak?.longestStreak ?: 0} ${uiState.streak?.unitName ?: "days"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Longest Streak",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${uiState.stats?.totalCompletions ?: 0}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Total Completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Completion Rates Row
                val isTimesPerWeek = habit.schedule is Schedule.TimesPerWeek
                val rate1Label = if (isTimesPerWeek) "4 Weeks" else "7 Days"
                val rate2Label = if (isTimesPerWeek) "12 Weeks" else "30 Days"

                val rate1Val = uiState.stats?.completionRate7d?.let { String.format(Locale.US, "%.0f%%", it) } ?: "—"
                val rate2Val = uiState.stats?.completionRate30d?.let { String.format(Locale.US, "%.0f%%", it) } ?: "—"
                val rateAllVal = uiState.stats?.completionRateAllTime?.let { String.format(Locale.US, "%.0f%%", it) } ?: "—"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(label = rate1Label, value = rate1Val, modifier = Modifier.weight(1f))
                    StatTile(label = rate2Label, value = rate2Val, modifier = Modifier.weight(1f))
                    StatTile(label = "All Time", value = rateAllVal, modifier = Modifier.weight(1f))
                }

                // Month Calendar Section
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "History & Calendar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MonthCalendarView(
                            habit = habit,
                            dayInfos = uiState.monthDays,
                            selectedMonth = uiState.yearMonth,
                            today = today,
                            onMonthChanged = {
                                if (it > uiState.yearMonth) viewModel.nextMonth() else viewModel.previousMonth()
                            },
                            onDayClicked = { clickedDate ->
                                val day = uiState.monthDays.find { it.date == clickedDate }
                                if (day != null) viewModel.selectDayToEdit(day)
                            }
                        )
                    }
                }

                // Weekly Bar Chart Section (12 weeks)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Last 12 Weeks Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        WeeklyBarChart(bars = uiState.weeklyBars)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Day Edit Bottom Sheet
        uiState.selectedDayToEdit?.let { dayInfo ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissEditDay() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                DayCorrectionSheetContent(
                    habit = habit ?: return@ModalBottomSheet,
                    dayInfo = dayInfo,
                    onSave = { viewModel.saveDayLog(it) },
                    onClear = { viewModel.saveDayLog(0) },
                    onDismiss = { viewModel.dismissEditDay() }
                )
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog && habit != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Habit?") },
                text = { Text("Are you sure you want to delete '${habit.name}'? All history and logs will be permanently deleted.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteHabit(onDeleted)
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                cancelButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayCorrectionSheetContent(
    habit: Habit,
    dayInfo: DayInfo,
    onSave: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var currentValue by remember { mutableStateOf(dayInfo.value) }
    var textInput by remember { mutableStateOf(dayInfo.value.toString()) }

    val formattedDate = dayInfo.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Edit Log",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        when (habit.type) {
            HabitType.YES_NO -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark Missed")
                    }
                    Button(
                        onClick = { onSave(1) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark Done")
                    }
                }
            }
            HabitType.COUNTABLE, HabitType.TIMED -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                val next = (currentValue - habit.step).coerceAtLeast(0)
                                currentValue = next
                                textInput = next.toString()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", style = MaterialTheme.typography.titleLarge)
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = {
                            textInput = it.filter { c -> c.isDigit() }
                            currentValue = textInput.toIntOrNull() ?: 0
                        },
                        modifier = Modifier.width(110.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                        singleLine = true
                    )

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                val next = currentValue + habit.step
                                currentValue = next
                                textInput = next.toString()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    OutlinedButton(
                        onClick = {
                            currentValue = habit.target
                            textInput = habit.target.toString()
                        }
                    ) {
                        Text("Set Target (${habit.target})")
                    }
                    OutlinedButton(onClick = onClear) {
                        Text("Clear")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(onClick = { onSave(currentValue) }, modifier = Modifier.weight(1f)) {
                        Text("Save")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
