package com.peppedess.wattmeter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peppedess.wattmeter.UiState
import com.peppedess.wattmeter.battery.Format
import com.peppedess.wattmeter.battery.SessionRecord
import com.peppedess.wattmeter.ui.components.PillItem
import com.peppedess.wattmeter.ui.components.PillRow
import com.peppedess.wattmeter.ui.components.QuietRow
import com.peppedess.wattmeter.ui.theme.energyColors
import java.util.Calendar
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: UiState,
    onBack: () -> Unit,
    onClear: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val energy = energyColors()
    val stats = state.historyStats
    val records = state.records

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Storico", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    if (records.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Svuota storico",
                                tint = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessuna ricarica archiviata.\nLe sessioni compaiono qui quando " +
                            "durano almeno due minuti.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PillRow(
                    items = listOf(
                        PillItem(
                            value = stats.count.toString(),
                            unit = "",
                            label = "ricariche",
                            container = scheme.primaryContainer,
                            content = scheme.onPrimaryContainer
                        ),
                        PillItem(
                            value = Format.number(stats.totalEnergyWh, 1),
                            unit = "Wh",
                            label = "totali",
                            container = scheme.secondaryContainer,
                            content = scheme.onSecondaryContainer
                        ),
                        PillItem(
                            value = Format.watt(stats.bestPeakW),
                            unit = "W",
                            label = "picco",
                            container = scheme.tertiaryContainer,
                            content = scheme.onTertiaryContainer
                        )
                    )
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    color = scheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Energia per ricarica",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(10.dp))
                        EnergyBars(
                            records = records.take(12).reversed(),
                            color = energy.fast
                        )
                        Spacer(Modifier.height(12.dp))
                        QuietRow("Potenza media", "${Format.watt(stats.averagePowerW)} W")
                        QuietRow(
                            "Velocità media",
                            Format.percentPerHour(stats.averagePercentPerHour)
                        )
                        QuietRow("Durata media", Format.duration(stats.averageDurationMs))
                        QuietRow("Temperatura massima", Format.celsius(stats.maxTemperatureC))
                    }
                }
            }

            items(records) { record ->
                RecordCard(record = record, accent = energy.fast)
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun RecordCard(record: SessionRecord, accent: Color) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = scheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayLabel(record.startTime),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${record.startLevel}% → ${record.endLevel}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${Format.duration(record.durationMs)} · " +
                        "${Format.wattHour(record.energyWh)} · " +
                        "picco ${Format.watt(record.peakPowerW)} W · ${record.source}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

/** Barre proporzionali all'energia di ogni ricarica, la più recente a destra. */
@Composable
private fun EnergyBars(records: List<SessionRecord>, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        if (records.isEmpty()) return@Canvas
        val peak = max(records.maxOf { it.energyWh }, 0.1f)
        val gap = size.width * 0.02f
        val barWidth = (size.width - gap * (records.size - 1)) / records.size

        records.forEachIndexed { index, record ->
            val x = index * (barWidth + gap)
            val ratio = (record.energyWh / peak).coerceIn(0.04f, 1f)
            val barHeight = size.height * ratio

            drawRoundRect(
                color = track,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3f)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3f)
            )
        }
    }
}

private fun dayLabel(timestamp: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = timestamp }
    val giorni = listOf("dom", "lun", "mar", "mer", "gio", "ven", "sab")
    val giorno = giorni[(c.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
    return String.format(
        java.util.Locale.ITALY,
        "%s %d/%d · %02d:%02d",
        giorno,
        c.get(Calendar.DAY_OF_MONTH),
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.HOUR_OF_DAY),
        c.get(Calendar.MINUTE)
    )
}
