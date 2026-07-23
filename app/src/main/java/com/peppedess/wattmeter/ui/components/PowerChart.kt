package com.peppedess.wattmeter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

/**
 * Andamento della potenza negli ultimi campioni.
 * I valori positivi sono energia in ingresso, i negativi consumo a batteria.
 */
@Composable
fun PowerChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp
) {
    val line = MaterialTheme.colorScheme.primary
    val fillTop = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val fillBottom = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val negative = MaterialTheme.colorScheme.error

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        if (values.size < 2) {
            Text(
                text = "Raccolta dei campioni in corso…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxValue = max(values.maxOrNull() ?: 1f, 1f)
            val minValue = minOf(values.minOrNull() ?: 0f, 0f)
            val range = max(maxValue - minValue, 0.5f)

            val zeroY = size.height * (maxValue / range)

            drawLine(
                color = grid,
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )

            val stepX = size.width / (values.size - 1).toFloat()
            fun yFor(value: Float): Float =
                size.height * ((maxValue - value) / range)

            val linePath = Path()
            val fillPath = Path()
            values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = yFor(value)
                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, zeroY)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo((values.size - 1) * stepX, zeroY)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(listOf(fillTop, fillBottom))
            )

            drawPath(
                path = linePath,
                color = if ((values.lastOrNull() ?: 0f) < 0f) negative else line,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )

            val lastValue = values.last()
            drawCircle(
                color = if (lastValue < 0f) negative else line,
                radius = 6f,
                center = Offset((values.size - 1) * stepX, yFor(lastValue))
            )
            drawCircle(
                color = if (lastValue < 0f) negative else line,
                radius = 14f,
                center = Offset((values.size - 1) * stepX, yFor(lastValue)),
                alpha = 0.22f
            )
        }
    }
}

/** Etichette min/max per il grafico. */
fun chartBounds(values: List<Float>): Pair<Float, Float> {
    if (values.isEmpty()) return 0f to 0f
    val maxValue = values.maxOrNull() ?: 0f
    val minValue = values.minOrNull() ?: 0f
    return (if (abs(minValue) < 0.01f) 0f else minValue) to maxValue
}
