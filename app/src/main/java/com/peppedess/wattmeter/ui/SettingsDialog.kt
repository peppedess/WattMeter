package com.peppedess.wattmeter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peppedess.wattmeter.battery.CurrentUnit

@Composable
fun SettingsDialog(
    currentUnit: CurrentUnit,
    autoStart: Boolean,
    dynamicColor: Boolean,
    onUnitChange: (CurrentUnit) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onResetRecords: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Impostazioni") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        },
        dismissButton = {
            TextButton(onClick = onResetRecords) { Text("Azzera record") }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Unità della corrente",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Alcuni produttori dichiarano microampere ma restituiscono " +
                            "milliampere. Se i watt sembrano mille volte sbagliati, " +
                            "forza l'unità corretta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.selectableGroup()) {
                    CurrentUnit.entries.forEach { unit ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = unit == currentUnit,
                                onClick = { onUnitChange(unit) }
                            )
                            Text(
                                text = unit.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Avvio automatico",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Mostra la notifica live appena colleghi il caricatore",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = autoStart, onCheckedChange = onAutoStartChange)
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Colori dallo sfondo",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Sostituisce la palette dell'app con quella estratta " +
                                    "dal tuo sfondo: se e neutro, l'app diventa grigia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange)
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Come vengono calcolati i dati",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "La potenza è il prodotto della tensione di cella per la corrente " +
                            "letta dal driver della batteria: è quindi l'energia che entra " +
                            "davvero nella cella, non quella dichiarata dal caricatore. " +
                            "Fra dissipazione del cavo e conversione interna la differenza " +
                            "è normalmente del 10-20%.\n\n" +
                            "La capacità totale viene ricavata dal contatore di carica diviso " +
                            "per la percentuale attuale, quindi si stabilizza dopo qualche " +
                            "minuto di utilizzo.\n\n" +
                            "Il tempo rimanente tiene conto del rallentamento fisiologico " +
                            "sopra l'80%, dove la ricarica passa a tensione costante e la " +
                            "corrente cala progressivamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
