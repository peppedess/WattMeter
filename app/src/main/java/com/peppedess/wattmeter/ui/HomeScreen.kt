package com.peppedess.wattmeter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peppedess.wattmeter.UiState
import com.peppedess.wattmeter.battery.Format
import com.peppedess.wattmeter.ui.components.PillRow
import com.peppedess.wattmeter.ui.components.PowerChart
import com.peppedess.wattmeter.ui.components.PowerGauge
import com.peppedess.wattmeter.ui.components.QuietRow
import com.peppedess.wattmeter.ui.components.StatBlock
import com.peppedess.wattmeter.ui.components.StatusChip
import com.peppedess.wattmeter.ui.components.peakOf
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState,
    onOpenSettings: () -> Unit,
    onToggleService: () -> Unit,
    onResetSession: () -> Unit
) {
    val reading = state.reading
    val estimate = state.estimate
    val session = state.session
    val scheme = MaterialTheme.colorScheme

    val accentTarget = when {
        reading.isFull -> scheme.primary
        reading.isCharging && reading.powerW >= 15f -> scheme.primary
        reading.isCharging -> scheme.tertiary
        reading.powerW >= 4f -> scheme.error
        else -> scheme.onSurfaceVariant
    }
    val accent by animateColorAsState(accentTarget, tween(600), label = "screenAccent")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("WattMeter", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onToggleService) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifica persistente",
                            tint = if (state.serviceRunning) accent else scheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Info, contentDescription = "Impostazioni")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PowerGauge(
                        levelPercent = reading.levelPercent,
                        signedPowerW = reading.signedPowerW,
                        charging = reading.isCharging,
                        full = reading.isFull
                    )
                    StatusChip(text = reading.shortStatus, accent = accent)
                }
            }

            item {
                val headline = when {
                    estimate.toFullMs != null -> Format.duration(estimate.toFullMs)
                    estimate.toEmptyMs != null -> Format.duration(estimate.toEmptyMs)
                    else -> null
                }
                AnimatedVisibility(
                    visible = headline != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(30.dp),
                        color = accent.copy(alpha = 0.13f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = headline ?: "",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.onSurface
                                )
                                Text(
                                    text = if (estimate.toFullMs != null) {
                                        "alla carica completa"
                                    } else {
                                        "di autonomia"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            if (estimate.toFullMs != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = Format.clockTime(
                                            reading.timestamp,
                                            estimate.toFullMs
                                        ),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accent
                                    )
                                    if (estimate.toEightyMs != null) {
                                        Text(
                                            text = "80% alle ${
                                                Format.clockTime(
                                                    reading.timestamp,
                                                    estimate.toEightyMs
                                                )
                                            }",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = scheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = scheme.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "picco ${Format.watt(peakOf(state.history))} W",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                        PowerChart(values = state.history, accent = accent)
                    }
                }
            }

            item {
                PillRow(
                    items = listOf(
                        Triple(
                            Format.number(reading.absCurrentMa, 0),
                            "mA",
                            "corrente"
                        ),
                        Triple(
                            Format.number(reading.voltageV, 2),
                            "V",
                            "tensione"
                        ),
                        Triple(
                            Format.number(reading.temperatureC, 1),
                            "°C",
                            "temperatura"
                        )
                    )
                )
            }

            item {
                PillRow(
                    items = listOf(
                        Triple(
                            Format.number(reading.chargeCounterMah ?: 0f, 0),
                            "mAh",
                            "residui"
                        ),
                        Triple(
                            Format.number(reading.fullCapacityMah ?: 0f, 0),
                            "mAh",
                            "capacità"
                        ),
                        Triple(
                            state.recordPowerW.let { Format.watt(it) },
                            "W",
                            "record"
                        )
                    )
                )
            }

            item {
                AnimatedVisibility(
                    visible = session.active,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = scheme.surfaceContainerLow
                    ) {
                        Column(modifier = Modifier.padding(vertical = 18.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sessione",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(onClick = onResetSession) {
                                    Icon(
                                        Icons.Filled.Refresh,
                                        contentDescription = "Azzera sessione",
                                        tint = scheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatBlock(
                                    value = Format.duration(session.durationMs),
                                    label = "durata"
                                )
                                StatBlock(
                                    value = "+${session.gainedPercent}%",
                                    label = "guadagnati"
                                )
                                StatBlock(
                                    value = Format.wattHour(session.energyWh),
                                    label = "accumulati"
                                )
                            }
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                QuietRow(
                                    "Potenza media",
                                    "${Format.watt(session.averagePowerW)} W"
                                )
                                QuietRow(
                                    "Picco",
                                    "${Format.watt(session.peakPowerW)} W · ${
                                        Format.milliAmp(session.peakCurrentMa)
                                    }"
                                )
                                QuietRow(
                                    "Velocità media",
                                    Format.percentPerHour(session.percentPerHour)
                                )
                                QuietRow(
                                    "Temperatura massima",
                                    Format.celsius(session.maxTemperatureC)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = scheme.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        QuietRow("Salute", reading.healthLabel)
                        reading.stateOfHealthPercent?.let {
                            QuietRow("Capacità nominale", "$it%")
                        }
                        reading.cycleCount?.let {
                            QuietRow("Cicli", it.toString())
                        }
                        QuietRow("Tecnologia", reading.technology)
                        if (abs(reading.averageCurrentMa) > 1f) {
                            QuietRow(
                                "Corrente media",
                                Format.milliAmp(abs(reading.averageCurrentMa))
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
