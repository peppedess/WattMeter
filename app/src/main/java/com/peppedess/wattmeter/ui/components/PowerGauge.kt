package com.peppedess.wattmeter.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Anello di avanzamento della carica con il valore di potenza istantanea al centro.
 * Disegnato interamente su Canvas per non dipendere da API sperimentali instabili.
 */
@Composable
fun PowerGauge(
    levelPercent: Int,
    powerW: Float,
    charging: Boolean,
    statusLabel: String,
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = levelPercent / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "level"
    )
    val animatedPower by animateFloatAsState(
        targetValue = powerW,
        animationSpec = tween(durationMillis = 450),
        label = "power"
    )

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val idle = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.25f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1.25f)) {
            val strokeWidth = size.minDimension * 0.085f
            val inset = strokeWidth / 2f + size.minDimension * 0.06f
            val diameter = size.minDimension - inset * 2f
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f + size.height * 0.04f
            )
            val arcSize = Size(diameter, diameter)
            val startAngle = 135f
            val sweepTotal = 270f

            drawArc(
                color = track,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            val brush = Brush.sweepGradient(
                colors = if (charging) {
                    listOf(tertiary, primary, tertiary)
                } else {
                    listOf(idle, idle)
                }
            )

            drawArc(
                brush = brush,
                startAngle = startAngle,
                sweepAngle = sweepTotal * animatedLevel.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Tacche ogni 10% lungo l'arco
            val center = Offset(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)
            val radius = diameter / 2f
            for (i in 0..10) {
                val angleDeg = startAngle + sweepTotal * i / 10f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val outer = radius + strokeWidth * 0.85f
                val inner = radius + strokeWidth * 0.45f
                drawLine(
                    color = track.copy(alpha = 0.85f),
                    start = Offset(
                        center.x + (inner * kotlin.math.cos(angleRad)).toFloat(),
                        center.y + (inner * kotlin.math.sin(angleRad)).toFloat()
                    ),
                    end = Offset(
                        center.x + (outer * kotlin.math.cos(angleRad)).toFloat(),
                        center.y + (outer * kotlin.math.sin(angleRad)).toFloat()
                    ),
                    strokeWidth = size.minDimension * 0.008f,
                    cap = StrokeCap.Round
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(
                text = com.peppedess.wattmeter.battery.Format.watt(animatedPower),
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                color = if (charging) MaterialTheme.colorScheme.primary else idle,
                textAlign = TextAlign.Center
            )
            Text(
                text = "watt in ingresso",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$levelPercent%  ·  $statusLabel",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

/** Colore semantico per la velocità di ricarica. */
@Composable
fun speedColor(powerW: Float, charging: Boolean): Color = when {
    !charging -> MaterialTheme.colorScheme.onSurfaceVariant
    powerW >= 20f -> MaterialTheme.colorScheme.primary
    powerW >= 7f -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.secondary
}
