package com.tidemark.habits.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidemark.habits.R
import com.tidemark.habits.domain.model.Habit
import com.tidemark.habits.domain.model.HabitType
import com.tidemark.habits.domain.model.Schedule
import com.tidemark.habits.ui.components.EmptyStateView
import com.tidemark.habits.ui.components.ProgressRing
import com.tidemark.habits.ui.components.StreakChip
import com.tidemark.habits.ui.components.WaveHeader
import com.tidemark.habits.ui.theme.LocalTidemarkColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onNavigateToHabitDetail: (Long) -> Unit,
    onNavigateToCreateHabit: (prefill: String?) -> Unit,
    onNavigateToEditHabit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostStateState() }
    val scope = rememberCoroutineScope()

    var sheetHabit by remember { mutableStateOf<Habit?>(null) }
    var sheetProgressValue by remember { mutableStateOf(0) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Wave Header with Title, Progress, and Add Button
            val isToday = uiState.selectedDate == uiState.currentDate
            val headerTitle = if (isToday) {
                stringResource(R.string.today_title)
            } else {
                uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))
            }

            val summaryText = if (uiState.totalScheduledCount > 0) {
                stringResource(
                    R.string.today_summary_done,
                    uiState.completedCount,
                    uiState.totalScheduledCount
                )
            } else {
                stringResource(R.string.today_summary_none)
            }

            WaveHeader(
                title = headerTitle,
                subtitle = summaryText,
                onAddClick = { onNavigateToCreateHabit(null) }
            )

            // Date Strip (14 days)
            DateStrip(
                stripItems = uiState.dateStrip,
                selectedDate = uiState.selectedDate,
                onSelectDate = { viewModel.selectDate(it) }
            )

            // Banner if viewing past date
            AnimatedVisibility(
                visible = !isToday,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.today_banner_editing_past,
                            uiState.selectedDate.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { viewModel.selectDate(uiState.currentDate) }) {
                        Text(
                            text = stringResource(R.string.today_banner_back_to_today),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Habits List or Empty State
            if (uiState.items.isEmpty()) {
                EmptyStateView(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.today_empty_title),
                    subtitle = stringResource(R.string.today_empty_subtitle),
                    quickStarts = listOf("Read", "Drink water", "Gym", "Meditate", "Study"),
                    onQuickStartSelected = { onNavigateToCreateHabit(it) },
                    onCreateCustom = { onNavigateToCreateHabit(null) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.items, key = { it.habit.id }) { item ->
                        TodayHabitCard(
                            item = item,
                            activeTimer = uiState.activeTimer,
                            elapsedSeconds = uiState.elapsedTimerSeconds,
                            onToggleYesNo = {
                                viewModel.toggleYesNo(item.habit)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Habit updated",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoLastToggle()
                                    }
                                }
                            },
                            onQuickAdd = { viewModel.quickAdd(item.habit) },
                            onStartTimer = { viewModel.startStopwatch(item.habit.id) },
                            onStopTimer = { viewModel.stopStopwatch(item.habit) },
                            onAddManualMinutes = { viewModel.addManualMinutes(item.habit, 5) },
                            onOpenSheet = {
                                sheetHabit = item.habit
                                sheetProgressValue = item.progressValue
                            },
                            onNavigateToDetail = { onNavigateToHabitDetail(item.habit.id) },
                            onNavigateToEdit = { onNavigateToEditHabit(item.habit.id) }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Stepper / Value adjustment
    sheetHabit?.let { habit ->
        ModalBottomSheet(
            onDismissRequest = { sheetHabit = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            HabitEditValueSheetContent(
                habit = habit,
                initialValue = sheetProgressValue,
                onSaveValue = { newValue ->
                    viewModel.setLogValue(habit, newValue)
                    sheetHabit = null
                },
                onClear = {
                    viewModel.setLogValue(habit, 0)
                    sheetHabit = null
                },
                onDismiss = { sheetHabit = null }
            )
        }
    }
}

private fun SnackbarHostStateState() = SnackbarHostState()

@Composable
private fun DateStrip(
    stripItems: List<DateStripItem>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stripItems, key = { it.date.toString() }) { item ->
            DateStripPill(
                item = item,
                isSelected = item.date == selectedDate,
                onClick = { onSelectDate(item.date) }
            )
        }
    }
}

@Composable
private fun DateStripPill(
    item: DateStripItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dayOfWeek = item.date.dayOfWeek.name.take(1)
    val dayOfMonth = item.date.dayOfMonth.toString()

    val colors = LocalTidemarkColors.current
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .width(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = dayOfMonth,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Completion dot
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(
                    if (item.isAllCompleted) {
                        if (isSelected) MaterialTheme.colorScheme.onPrimary else colors.coral
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    }
                )
        )
    }
}

@Composable
private fun TodayHabitCard(
    item: TodayHabitItem,
    activeTimer: com.tidemark.habits.data.settings.ActiveTimer?,
    elapsedSeconds: Long,
    onToggleYesNo: () -> Unit,
    onQuickAdd: () -> Unit,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onAddManualMinutes: () -> Unit,
    onOpenSheet: () -> Unit,
    onNavigateToDetail: () -> Unit,
    onNavigateToEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isTimerRunning = activeTimer != null && activeTimer.habitId == item.habit.id

    val habit = item.habit
    val progressFraction = if (habit.target > 0) {
        (item.progressValue.toFloat() / habit.target.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val subtitle = when (habit.type) {
        HabitType.YES_NO -> if (item.isCompleted) "Done" else "Not done yet"
        HabitType.COUNTABLE -> "${item.progressValue} / ${habit.target} ${habit.unit ?: ""}".trim()
        HabitType.TIMED -> if (isTimerRunning) {
            val mins = elapsedSeconds / 60
            val secs = elapsedSeconds % 60
            String.format(Locale.US, "%02d:%02d running", mins, secs)
        } else {
            "${item.progressValue} / ${habit.target} min"
        }
    }

    val accessibilityLabel = "${habit.name}, $subtitle, streak ${item.streak.currentStreak} ${item.streak.unitName}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilityLabel }
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenSheet),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left interactive ring / check
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable {
                            if (habit.type == HabitType.YES_NO) {
                                onToggleYesNo()
                            } else {
                                onOpenSheet()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    ProgressRing(
                        progress = if (item.isCompleted) 1f else progressFraction,
                        isCompleted = item.isCompleted,
                        size = 40.dp,
                        strokeWidth = 3.5.dp
                    )
                    if (item.isCompleted) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Streak Chip
                StreakChip(
                    count = item.streak.currentStreak,
                    unit = item.streak.unitName
                )

                // Overflow menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "More actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View details") },
                            onClick = {
                                showMenu = false
                                onNavigateToDetail()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit habit") },
                            onClick = {
                                showMenu = false
                                onNavigateToEdit()
                            }
                        )
                    }
                }
            }

            // Quick action controls row for Countable and Timed
            when (habit.type) {
                HabitType.COUNTABLE -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onQuickAdd,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(
                                text = "+${habit.step} ${habit.unit ?: ""}".trim(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                HabitType.TIMED -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onAddManualMinutes,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(text = "+5 min", style = MaterialTheme.typography.labelMedium)
                        }

                        if (isTimerRunning) {
                            Button(
                                onClick = onStopTimer,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text(
                                    text = "Stop",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = onStartTimer,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_timer),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Start",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                HabitType.YES_NO -> Unit
            }
        }
    }
}

@Composable
private fun HabitEditValueSheetContent(
    habit: Habit,
    initialValue: Int,
    onSaveValue: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var currentValue by remember { mutableStateOf(initialValue) }
    var textInput by remember { mutableStateOf(initialValue.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = habit.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Target: ${habit.target} ${habit.unit ?: "units"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Stepper: - / +
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledIconButton(
                onClick = {
                    val next = (currentValue - habit.step).coerceAtLeast(0)
                    currentValue = next
                    textInput = next.toString()
                }
            ) {
                Text(text = "−", style = MaterialTheme.typography.titleLarge)
            }

            OutlinedTextField(
                value = textInput,
                onValueChange = { input ->
                    textInput = input.filter { it.isDigit() }
                    currentValue = textInput.toIntOrNull() ?: 0
                },
                modifier = Modifier.width(110.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                singleLine = true
            )

            FilledIconButton(
                onClick = {
                    val next = currentValue + habit.step
                    currentValue = next
                    textInput = next.toString()
                }
            ) {
                Text(text = "+", style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick set buttons
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
                Text("Set to target")
            }
            OutlinedButton(onClick = onClear) {
                Text("Clear")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save & Cancel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { onSaveValue(currentValue) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
