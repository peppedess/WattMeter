package com.peppedess.wattmeter.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Una metrica con il proprio colore di superficie. */
@Immutable
data class PillItem(
    val value: String,
    val unit: String,
    val label: String,
    val container: Color,
    val content: Color
)

@Composable
fun MetricPill(item: PillItem, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = item.container
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = item.value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = item.content,
                    maxLines = 1
                )
                if (item.unit.isNotEmpty()) {
                    Text(
                        text = item.unit,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = item.content.copy(alpha = 0.75f),
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                    )
                }
            }
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = item.content.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
fun PillRow(items: List<PillItem>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            MetricPill(item = item, modifier = Modifier.weight(1f))
        }
    }
}

/** Etichetta di stato piena, non trasparente: deve saltare all'occhio. */
@Composable
fun StatusChip(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(container, tween(600), label = "chipBg")
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(content)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = content
            )
        }
    }
}

/** Numero grande con didascalia, per le statistiche di sessione. */
@Composable
fun StatBlock(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** Riga discreta chiave/valore per i dettagli secondari. */
@Composable
fun QuietRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
