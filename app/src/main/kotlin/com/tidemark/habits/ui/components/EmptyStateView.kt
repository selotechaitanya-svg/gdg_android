package com.tidemark.habits.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tidemark.habits.R
import com.tidemark.habits.domain.model.HabitType

data class QuickStartHabit(
    val name: String,
    val type: HabitType,
    val target: Int,
    val unit: String?,
    val step: Int
)

val QuickStartIdeas = listOf(
    QuickStartHabit("Read", HabitType.COUNTABLE, 20, "pages", 5),
    QuickStartHabit("Drink water", HabitType.COUNTABLE, 8, "glasses", 1),
    QuickStartHabit("Gym", HabitType.YES_NO, 1, null, 1),
    QuickStartHabit("Meditate", HabitType.TIMED, 15, "min", 5),
    QuickStartHabit("Study", HabitType.TIMED, 45, "min", 15)
)

/**
 * Signature empty state illustration drawn on Canvas with an animated gentle drift.
 */
@Composable
fun SeaShellWaveIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_drift")
    val driftOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift_offset"
    )

    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier.size(100.dp)) {
        val w = size.width
        val h = size.height

        // Outer shell arch
        val shellPath = Path().apply {
            moveTo(w * 0.2f, h * 0.7f)
            cubicTo(w * 0.2f, h * 0.25f, w * 0.8f, h * 0.25f, w * 0.8f, h * 0.7f)
        }
        drawPath(shellPath, color = outline, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        // Inner radiating ribs
        val centerBaseX = w * 0.5f
        val centerBaseY = h * 0.7f

        drawLine(primary, start = androidx.compose.ui.geometry.Offset(centerBaseX, centerBaseY), end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.25f), strokeWidth = 2.5f)
        drawLine(primary.copy(alpha = 0.7f), start = androidx.compose.ui.geometry.Offset(centerBaseX, centerBaseY), end = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.3f), strokeWidth = 2.5f)
        drawLine(primary.copy(alpha = 0.7f), start = androidx.compose.ui.geometry.Offset(centerBaseX, centerBaseY), end = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.3f), strokeWidth = 2.5f)

        // Drifting water ripple below
        val wavePath = Path().apply {
            moveTo(w * 0.15f + driftOffset, h * 0.85f)
            cubicTo(
                w * 0.35f + driftOffset, h * 0.78f,
                w * 0.65f + driftOffset, h * 0.92f,
                w * 0.85f + driftOffset, h * 0.85f
            )
        }
        drawPath(wavePath, color = primary.copy(alpha = 0.5f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    onSelectIdea: (QuickStartHabit) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SeaShellWaveIllustration()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Quick-start templates:",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (idea in QuickStartIdeas) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onSelectIdea(idea) }
                ) {
                    Text(
                        text = "+ ${idea.name}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
