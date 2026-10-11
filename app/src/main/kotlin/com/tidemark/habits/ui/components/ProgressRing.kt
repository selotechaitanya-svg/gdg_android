package com.tidemark.habits.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tidemark.habits.ui.theme.LocalTidemarkColors

/**
 * Animated circular progress ring with integrated checkmark animation.
 *
 * @param progress Ratio of current progress to target [0f, 1f].
 * @param isCompleted True when progress meets or exceeds target.
 * @param size Diameter of the ring (minimum 48 dp for accessibility touch targets).
 * @param strokeWidth Thickness of the ring stroke.
 * @param onClick Optional tap handler for toggling completion.
 */
@Composable
fun ProgressRing(
    progress: Float,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
    onClick: (() -> Unit)? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = LocalTidemarkColors.current.track

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "progress_ring_animation"
    )

    val checkAnim by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "checkmark_animation"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = rememberRipple(bounded = false, radius = size / 2),
            role = Role.Checkbox,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size - 8.dp)) {
            val strokePx = strokeWidth.toPx()
            val canvasSize = this.size.minDimension
            val radius = (canvasSize - strokePx) / 2f
            val center = this.center

            // Background track ring
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokePx)
            )

            // Active progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            // Fill & Checkmark on completion
            if (checkAnim > 0f) {
                drawCircle(
                    color = primaryColor.copy(alpha = checkAnim),
                    radius = radius,
                    center = center
                )

                // Draw checkmark tick
                val checkPath = Path().apply {
                    val leftX = center.x - radius * 0.35f
                    val leftY = center.y
                    val midX = center.x - radius * 0.05f
                    val midY = center.y + radius * 0.35f
                    val rightX = center.x + radius * 0.45f
                    val rightY = center.y - radius * 0.35f

                    moveTo(leftX, leftY)
                    lineTo(midX, midY)
                    lineTo(rightX, rightY)
                }
                drawPath(
                    path = checkPath,
                    color = Color.White,
                    style = Stroke(width = strokePx * 1.1f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
