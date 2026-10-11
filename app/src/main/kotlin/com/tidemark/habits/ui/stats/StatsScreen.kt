package com.tidemark.habits.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidemark.habits.R
import com.tidemark.habits.ui.components.EmptyStateView
import com.tidemark.habits.ui.components.StreakChip
import com.tidemark.habits.ui.theme.LocalTidemarkColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (!uiState.hasHabits) {
            EmptyStateView(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                title = "No statistics yet",
                subtitle = "Start completing daily habits to view trends and streaks over time."
            )
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
                // Overview 2x2 Grid
                val overview = uiState.overview
                val rate7dText = overview.rate7d?.let { String.format(Locale.US, "%.0f%%", it) } ?: "—"
                val rate30dText = overview.rate30d?.let { String.format(Locale.US, "%.0f%%", it) } ?: "—"
                val bestStreakText = if (overview.bestStreakCount > 0 && overview.bestStreakHabitName != null) {
                    "${overview.bestStreakCount} (${overview.bestStreakHabitName})"
                } else "—"

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewMetricTile(
                            label = "Done Today",
                            value = "${overview.completionsToday}",
                            modifier = Modifier.weight(1f)
                        )
                        OverviewMetricTile(
                            label = "7-Day Rate",
                            value = rate7dText,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewMetricTile(
                            label = "30-Day Rate",
                            value = rate30dText,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewMetricTile(
                            label = "Best Current Streak",
                            value = bestStreakText,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 7-Day Completion Bar Chart
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Completions (Last 7 Days)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        SevenDayBarChart(bars = uiState.daily7dBars)
                    }
                }

                // Habit Breakdown List
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Habits by Streak",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        uiState.habitRows.forEachIndexed { index, row ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            HabitStatRowItem(
                                row = row,
                                onClick = { onNavigateToDetail(row.habit.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun OverviewMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
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
private fun SevenDayBarChart(bars: List<Daily7dBar>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = LocalTidemarkColors.current.track

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (bars.isEmpty()) return@Canvas

                val barCount = bars.size
                val availableWidth = size.width
                val availableHeight = size.height

                val spacing = 12.dp.toPx()
                val barWidth = ((availableWidth - (spacing * (barCount - 1))) / barCount).coerceAtLeast(10.dp.toPx())

                bars.forEachIndexed { index, bar ->
                    val x = index * (barWidth + spacing)
                    val barHeight = availableHeight * bar.percentage

                    // Background track
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, availableHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Filled progress
                    if (barHeight > 0f) {
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(x, availableHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Weekday initials
        Row(modifier = Modifier.fillMaxWidth()) {
            bars.forEach { bar ->
                Text(
                    text = bar.date.dayOfWeek.name.take(1),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HabitStatRowItem(
    row: HabitStatsRow,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.habit.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Rate progress bar
            val rateFraction = row.rate30d ?: 0f
            LinearProgressIndicator(
                progress = { rateFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = LocalTidemarkColors.current.track,
            )
            Spacer(modifier = Modifier.height(2.dp))
            val rateStr = row.rate30d?.let { String.format(Locale.US, "%.0f%% 30d", it * 100f) } ?: "30d: —"
            Text(
                text = "$rateStr • ${row.totalCompletions} total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        StreakChip(
            count = row.streak.currentStreak,
            unit = row.streak.unitName
        )
    }
}
