package com.tidemark.habits.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Signature brand component: A soft oceanic wave curve along the bottom of the header.
 * Drawn purely via Compose Canvas without raster images.
 */
@Composable
fun WaveHeader(
    modifier: Modifier = Modifier,
    waveHeight: Dp = 18.dp,
    content: @Composable () -> Unit
) {
    val waveColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxWidth()) {
        content()

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(waveHeight)
                .align(Alignment.BottomCenter)
        ) {
            val width = size.width
            val height = size.height

            val path = Path().apply {
                moveTo(0f, 0f)
                cubicTo(
                    width * 0.25f, height * 0.85f,
                    width * 0.75f, -height * 0.15f,
                    width, height * 0.6f
                )
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(path, color = waveColor)
        }
    }
}
