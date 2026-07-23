package com.peppedess.wattmeter.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/** Andamento della potenza: sopra lo zero entra energia, sotto esce. */
@Composable
fun PowerChart(
    values: List<Float>,
    accent: Color,
    modifier: Modifier = Modifier,
    height: Dp = 132.dp
) {
    val scheme = MaterialTheme.colorScheme
    val grid = scheme.outlineVariant

    val transition = rememberInfiniteTransition(label = "tip")
    val beat by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beatValue"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        if (values.size < 3) {
            Text(
                text = "…",
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onSurfaceVariant
            )
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val top = max(values.maxOrNull() ?: 0f, 0.5f)
            val bottom = minOf(values.minOrNull() ?: 0f, 0f)
            val range = max(top - bottom, 0.6f)
            val pad = size.height * 0.08f
            val usable = size.height - pad * 2f

            fun yFor(v: Float): Float = pad + usable * ((top - v) / range)

            val zeroY = yFor(0f)
            val stepX = size.width / (values.size - 1).toFloat()

            if (bottom < -0.01f) {
                drawLine(
                    color = grid,
                    start = Offset(0f, zeroY),
                    end = Offset(size.width, zeroY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }

            val line = Path()
            val area = Path()
            var prevX = 0f
            var prevY = yFor(values[0])
            line.moveTo(prevX, prevY)
            area.moveTo(prevX, zeroY)
            area.lineTo(prevX, prevY)

            for (i in 1 until values.size) {
                val x = i * stepX
                val y = yFor(values[i])
                val midX = (prevX + x) / 2f
                line.cubicTo(midX, prevY, midX, y, x, y)
                area.cubicTo(midX, prevY, midX, y, x, y)
                prevX = x
                prevY = y
            }
            area.lineTo(prevX, zeroY)
            area.close()

            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.02f))
                )
            )
            drawPath(
                path = line,
                brush = Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.35f), accent)
                ),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            val pulse = 1f + sin(beat * 2f * Math.PI).toFloat() * 0.3f
            drawCircle(accent.copy(alpha = 0.20f), 15f * pulse, Offset(prevX, prevY))
            drawCircle(accent, 5.5f, Offset(prevX, prevY))
        }
    }
}

/** Valore massimo assoluto della serie, per l'etichetta del picco. */
fun peakOf(values: List<Float>): Float =
    values.maxOfOrNull { abs(it) } ?: 0f
