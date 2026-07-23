package com.peppedess.wattmeter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peppedess.wattmeter.UiState
import com.peppedess.wattmeter.battery.Format
import com.peppedess.wattmeter.ui.components.DetailRow
import com.peppedess.wattmeter.ui.components.MetricRow
import com.peppedess.wattmeter.ui.components.MetricTile
import com.peppedess.wattmeter.ui.components.PowerChart
import com.peppedess.wattmeter.ui.components.PowerGauge

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    state: UiState,
    onOpenSettings: () -> Unit,
    onToggleService: () -> Unit,
    onResetSession: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val reading = state.reading
    val estimate = state.estimate
    val session = state.session

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("WattMeter") },
                actions = {
                    IconButton(onClick = onToggleService) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifica persistente",
                            tint = if (state.serviceRunning) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Info, contentDescription = "Impostazioni e info")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PowerGauge(
                    levelPercent = reading.levelPercent,
                    powerW = reading.powerW,
                    charging = reading.isCharging,
                    statusLabel = reading.statusLabel
                )
            }

            item {
                LinearWavyProgressIndicator(
                    progress = { reading.levelPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = if (reading.isCharging) {
                                "Ricarica ${reading.speedLabel.lowercase()} · ${reading.sourceLabel}"
                            } else {
                                reading.sourceLabel
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = Format.duration(estimate.toFullMs),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (estimate.toFullMs != null) {
                                "al 100% · previsto alle ${
                                    Format.clockTime(reading.timestamp, estimate.toFullMs)
                                }"
                            } else {
                                "Stima non disponibile"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (estimate.toEightyMs != null) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.2f
                                )
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All'80%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${Format.duration(estimate.toEightyMs)} · ${
                                        Format.clockTime(reading.timestamp, estimate.toEightyMs)
                                    }",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Potenza · ultimi ${state.history.size} secondi",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        PowerChart(values = state.history)
                    }
                }
            }

            item {
                MetricRow(
                    leftLabel = "Corrente",
                    leftValue = Format.milliAmp(reading.absCurrentMa),
                    leftHint = "media ${Format.milliAmp(kotlin.math.abs(reading.averageCurrentMa))}",
                    rightLabel = "Tensione",
                    rightValue = Format.volt(reading.voltageV),
                    rightHint = "ai morsetti della cella"
                )
            }

            item {
                MetricRow(
                    leftLabel = "Temperatura",
                    leftValue = Format.celsius(reading.temperatureC),
                    leftHint = temperatureHint(reading.temperatureC),
                    rightLabel = "Carica residua",
                    rightValue = Format.mah(reading.chargeCounterMah),
                    rightHint = "su ${Format.mah(reading.fullCapacityMah)}"
                )
            }

            item {
                MetricRow(
                    leftLabel = "Da caricare",
                    leftValue = Format.mah(estimate.remainingMah),
                    leftHint = "${100 - reading.levelPercent}% mancante",
                    rightLabel = "Record potenza",
                    rightValue = "${Format.watt(state.recordPowerW)} W",
                    rightHint = "picco ${Format.milliAmp(state.recordCurrentMa)}"
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sessione di ricarica",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = onResetSession) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Azzera sessione")
                            }
                        }
                        if (!session.active) {
                            Text(
                                text = "Collega il caricatore per avviare il conteggio.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            DetailRow("Durata", Format.duration(session.durationMs))
                            DetailRow(
                                "Percentuale guadagnata",
                                "+${session.gainedPercent}% (da ${session.startLevel}%)"
                            )
                            DetailRow("Velocità media", Format.percentPerHour(session.percentPerHour))
                            DetailRow("Energia accumulata", Format.wattHour(session.energyWh))
                            DetailRow("Potenza media", "${Format.watt(session.averagePowerW)} W")
                            DetailRow("Potenza di picco", "${Format.watt(session.peakPowerW)} W")
                            DetailRow("Corrente di picco", Format.milliAmp(session.peakCurrentMa))
                            DetailRow("Temperatura massima", Format.celsius(session.maxTemperatureC))
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Dettagli batteria",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        DetailRow("Stato", reading.statusLabel)
                        DetailRow("Alimentazione", reading.sourceLabel)
                        DetailRow("Salute rilevata", reading.healthLabel)
                        reading.stateOfHealthPercent?.let {
                            DetailRow("Capacità residua nominale", "$it%")
                        }
                        reading.cycleCount?.let {
                            DetailRow("Cicli di carica", it.toString())
                        }
                        DetailRow("Tecnologia", reading.technology)
                        DetailRow("Capacità stimata", Format.mah(reading.fullCapacityMah))
                        DetailRow("Metodo di stima", estimate.source)
                        reading.systemEtaMs?.let {
                            DetailRow("Stima del sistema", Format.duration(it))
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onToggleService,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (state.serviceRunning) "Ferma notifica" else "Notifica live"
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Impostazioni")
                    }
                }
            }

            item {
                MetricTile(
                    label = "Nota sulla misura",
                    value = "Potenza alla batteria",
                    hint = "Il caricatore eroga il 10-20% in più per perdite di conversione",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun temperatureHint(value: Float): String = when {
    value >= 43f -> "molto calda"
    value >= 38f -> "calda"
    value >= 15f -> "normale"
    value > 0f -> "fredda"
    else -> "n/d"
}
