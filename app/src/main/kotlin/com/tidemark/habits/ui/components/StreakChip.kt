package com.tidemark.habits.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFeatureSettings
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tidemark.habits.ui.theme.LocalTidemarkColors

/**
 * Custom flame icon drawn in vector Canvas.
 */
@Composable
fun FlameIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalTidemarkColors.current.coral
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val flamePath = Path().apply {
            moveTo(w * 0.5f, 0f)
            cubicTo(w * 0.65f, h * 0.35f, w, h * 0.55f, w * 0.8f, h * 0.85f)
            cubicTo(w * 0.65f, h, w * 0.35f, h, w * 0.2f, h * 0.85f)
            cubicTo(0f, h * 0.55f, w * 0.35f, h * 0.35f, w * 0.5f, 0f)
            close()
        }
        drawPath(flamePath, color = tint)
    }
}

/**
 * Streak badge displaying the current streak count and unit ("5 d" or "3 wk").
 * Animates scale with a playful spring pop when count increases.
 */
@Composable
fun StreakChip(
    count: Int,
    unit: String,
    modifier: Modifier = Modifier
) {
    val coral = LocalTidemarkColors.current.coral
    val isZero = count == 0

    var prevCount by remember { mutableIntStateOf(count) }
    var popped by remember { mutableStateOf(false) }

    LaunchedEffect(count) {
        if (count > prevCount) {
            popped = true
            kotlinx.coroutines.delay(200)
            popped = false
        }
        prevCount = count
    }

    val scale by animateFloatAsState(
        targetValue = if (popped) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "streak_pop_animation"
    )

    val backgroundColor = if (isZero) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        coral.copy(alpha = 0.15f)
    }

    val contentColor = if (isZero) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        coral
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(backgroundColor, RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isZero) {
                FlameIcon(tint = contentColor)
                Spacer(modifier = Modifier.width(4.dp))
            }

            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
                    } else {
                        slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                    }
                },
                label = "streak_counter_roll"
            ) { targetCount ->
                Text(
                    text = "$targetCount $unit",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
