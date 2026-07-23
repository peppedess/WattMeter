package com.peppedess.wattmeter.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peppedess.wattmeter.battery.Format
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Anello del livello di carica con la potenza al centro.
 * Il segno indica la direzione: positivo entra nella batteria, negativo esce.
 */
@Composable
fun PowerGauge(
    levelPercent: Int,
    signedPowerW: Float,
    charging: Boolean,
    full: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val target = when {
        full -> scheme.primary
        charging && abs(signedPowerW) >= 15f -> scheme.primary
        charging -> scheme.tertiary
        abs(signedPowerW) >= 4f -> scheme.error
        else -> scheme.onSurfaceVariant
    }
    val accent by animateColorAsState(target, tween(600), label = "accent")

    val level by animateFloatAsState(
        targetValue = levelPercent / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "level"
    )
    val power by animateFloatAsState(
        targetValue = signedPowerW,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "power"
    )

    val transition = rememberInfiniteTransition(label = "flow")
    val flowRaw by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowValue"
    )
    val flow = if (charging && !full) flowRaw else -1f
    val track = scheme.surfaceContainerHighest

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.35f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.075f
            val diameter = size.minDimension - stroke * 2f - size.minDimension * 0.08f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val center = Offset(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)
            val radius = diameter / 2f
            val start = 130f
            val total = 280f

            drawArc(
                color = track,
                startAngle = start,
                sweepAngle = total,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            val filled = total * level.coerceIn(0f, 1f)

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(accent.copy(alpha = 0.5f), accent, accent.copy(alpha = 0.5f)),
                    center
                ),
                startAngle = start,
                sweepAngle = filled,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            if (flow >= 0f && filled > 14f) {
                val tail = 34f
                val head = start + filled * flow
                val fade = sin(flow * Math.PI).toFloat()
                val sweep = tail.coerceAtMost(head - start)
                if (sweep > 0f) {
                    drawArc(
                        color = accent.copy(alpha = 0.5f * fade),
                        startAngle = head - sweep,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke * 0.5f, cap = StrokeCap.Round)
                    )
                }
            }

            val tipAngle = Math.toRadians((start + filled).toDouble())
            val tip = Offset(
                center.x + (radius * cos(tipAngle)).toFloat(),
                center.y + (radius * sin(tipAngle)).toFloat()
            )
            val glow = if (charging) 1f + sin(flowRaw * 2f * Math.PI).toFloat() * 0.22f else 1f
            drawCircle(accent.copy(alpha = 0.16f), stroke * 1.1f * glow, tip)
            drawCircle(accent, stroke * 0.28f, tip)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = Format.signedWatt(power),
                    fontSize = 62.sp,
                    lineHeight = 66.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    text = "W",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Medium,
                    color = accent.copy(alpha = 0.75f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 9.dp)
                )
            }
            Text(
                text = "$levelPercent%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
