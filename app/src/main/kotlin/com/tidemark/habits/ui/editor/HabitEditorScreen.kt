package com.tidemark.habits.ui.editor

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidemark.habits.R
import com.tidemark.habits.domain.logic.HabitValidationError
import com.tidemark.habits.domain.model.HabitType
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditorScreen(
    viewModel: HabitEditorViewModel,
    onNavigateBack: () -> Unit,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.habitId == null) "New Habit" else "Edit Habit",
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
                    Button(
                        onClick = { viewModel.saveHabit(onSaved) },
                        enabled = uiState.isValid,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Name field
            Column {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { if (it.length <= 40) viewModel.updateName(it) },
                    label = { Text("Habit name") },
                    placeholder = { Text("e.g. Read, Drink water, Gym") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${uiState.name.length} / 40",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                uiState.validationErrors.filterIsInstance<HabitValidationError.BlankName>().forEach {
                    Text(
                        text = "Name cannot be blank",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Habit Type Segmented Buttons
            Column {
                Text(
                    text = "Habit Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val types = listOf(HabitType.YES_NO, HabitType.COUNTABLE, HabitType.TIMED)
                    types.forEachIndexed { index, type ->
                        val label = when (type) {
                            HabitType.YES_NO -> "Yes/No"
                            HabitType.COUNTABLE -> "Countable"
                            HabitType.TIMED -> "Timed"
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                            onClick = { viewModel.requestTypeChange(type) },
                            selected = uiState.type == type
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            // Type-specific target inputs
            when (uiState.type) {
                HabitType.YES_NO -> Unit
                HabitType.COUNTABLE -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Target and Unit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = uiState.target.toString(),
                                    onValueChange = {
                                        val v = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 1
                                        viewModel.updateTarget(v)
                                    },
                                    label = { Text("Daily target") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = uiState.unit,
                                    onValueChange = { if (it.length <= 12) viewModel.updateUnit(it) },
                                    label = { Text("Unit") },
                                    placeholder = { Text("pages, km…") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )
                            }

                            // Unit suggestions
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("pages", "glasses", "km", "steps", "reps").forEach { suggestion ->
                                    FilterChip(
                                        selected = uiState.unit == suggestion,
                                        onClick = { viewModel.updateUnit(suggestion) },
                                        label = { Text(suggestion) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            // Quick add step
                            OutlinedTextField(
                                value = uiState.step.toString(),
                                onValueChange = {
                                    val v = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 1
                                    viewModel.updateStep(v.coerceAtLeast(1))
                                },
                                label = { Text("Quick-add step amount (+step)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
                HabitType.TIMED -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = uiState.target.toString(),
                                onValueChange = {
                                    val v = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 1
                                    viewModel.updateTarget(v)
                                },
                                label = { Text("Target minutes") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            // Preset chips
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(15, 25, 45, 60).forEach { mins ->
                                    val chipLabel = if (mins == 25) "25 (Pomodoro)" else "$mins min"
                                    FilterChip(
                                        selected = uiState.target == mins,
                                        onClick = { viewModel.updateTarget(mins) },
                                        label = { Text(chipLabel) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Schedule Section
            Column {
                Text(
                    text = "Schedule",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val schedules = listOf("Daily", "Weekdays", "N× / week")
                    schedules.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = schedules.size),
                            onClick = { viewModel.updateScheduleTypeIndex(index) },
                            selected = uiState.scheduleTypeIndex == index
                        ) {
                            Text(label)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Weekday selection
                if (uiState.scheduleTypeIndex == 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = DayOfWeek.values()
                        days.forEach { day ->
                            val isSelected = uiState.weekdays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.toggleWeekday(day) },
                                label = { Text(day.name.take(1)) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Times per week selection
                if (uiState.scheduleTypeIndex == 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${uiState.timesPerWeek} days per week",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.updateTimesPerWeek(uiState.timesPerWeek - 1) },
                                enabled = uiState.timesPerWeek > 1
                            ) { Text("−") }
                            OutlinedButton(
                                onClick = { viewModel.updateTimesPerWeek(uiState.timesPerWeek + 1) },
                                enabled = uiState.timesPerWeek < 7
                            ) { Text("+") }
                        }
                    }
                }
            }

            // Reminder Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_bell),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Daily reminder",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = uiState.hasReminder,
                            onCheckedChange = { viewModel.toggleReminder(it) }
                        )
                    }

                    if (uiState.hasReminder) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            viewModel.updateReminderTime(LocalTime.of(hourOfDay, minute))
                                        },
                                        uiState.reminderTime.hour,
                                        uiState.reminderTime.minute,
                                        false
                                    ).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Reminder time",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = uiState.reminderTime.format(
                                    DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Type Change Confirmation Dialog
        if (uiState.showTypeChangeConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelTypeChange() },
                title = { Text("Change habit type?") },
                text = {
                    Text(
                        "Changing habit type adjusts past logs atomically:\n\n" +
                                "• To Countable/Timed: completed past days get target value.\n" +
                                "• To Yes/No: completed past days become 1, others 0.\n\n" +
                                "Existing streak history will be recalculated."
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.confirmTypeChange() }) {
                        Text("Confirm")
                    }
                },
                cancelButton = {
                    TextButton(onClick = { viewModel.cancelTypeChange() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
