package com.tidemark.habits.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tidemark.habits.domain.model.WeeklyBar
import com.tidemark.habits.ui.theme.LocalTidemarkColors

/**
 * Custom 12-week bar chart drawn directly on Canvas with staggered spring growth.
 */
@Composable
fun WeeklyBarChart(
    bars: List<WeeklyBar>,
    modifier: Modifier = Modifier,
    accessibleDescription: String = "12-week habit completion bar chart"
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = LocalTidemarkColors.current.track

    val animatedProgress = remember(bars) {
        bars.map { Animatable(0f) }
    }

    LaunchedEffect(bars) {
        bars.forEachIndexed { index, _ ->
            kotlinx.coroutines.launch {
                kotlinx.coroutines.delay((index * 25).toLong())
                animatedProgress[index].animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 350)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(vertical = 8.dp)
            .semantics { contentDescription = accessibleDescription }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            if (bars.isEmpty()) return@Canvas

            val barCount = bars.size
            val availableWidth = size.width
            val availableHeight = size.height

            val barSpacing = 8.dp.toPx()
            val totalSpacing = barSpacing * (barCount - 1)
            val barWidth = ((availableWidth - totalSpacing) / barCount).coerceAtLeast(6.dp.toPx())

            bars.forEachIndexed { index, bar ->
                val x = index * (barWidth + barSpacing)
                val fraction = (bar.percentage / 100.0).toFloat().coerceIn(0f, 1f)
                val animatedFraction = fraction * animatedProgress[index].value
                val barHeight = availableHeight * animatedFraction

                // Draw background track column
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, availableHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Draw filled progress column
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
}
