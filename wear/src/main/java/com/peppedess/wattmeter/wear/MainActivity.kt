package com.peppedess.wattmeter.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WatchScreen()
        }
    }
}

@Composable
private fun WatchScreen() {
    val context = LocalContext.current
    val store = remember { WatchStore.get(context) }

    // Nessun listener push disponibile qui: si rilegge quando la schermata
    // torna visibile e ogni volta che arriva un nuovo dato dal servizio.
    val flow = remember { MutableStateFlow(store.read()) }
    val snapshot by flow.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            flow.value = store.read()
            delay(2000L)
        }
    }

    var secondsAgo by remember { mutableLongStateOf(0L) }
    LaunchedEffect(snapshot.timestamp) {
        while (true) {
            secondsAgo = ((System.currentTimeMillis() - snapshot.timestamp) / 1000L)
                .coerceAtLeast(0L)
            delay(1000L)
        }
    }

    val accent = when {
        snapshot.timestamp == 0L -> Color(0xFF89998C)
        snapshot.charging -> Color(0xFF43E68A)
        else -> Color(0xFFFFB35C)
    }
    val background = Color(0xFF08150D)
    val textColor = Color(0xFFDFEDE2)
    val mutedColor = Color(0xFF89998C)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (snapshot.timestamp == 0L) {
                BasicText(
                    text = "In attesa del telefono",
                    style = TextStyle(
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )
                BasicText(
                    text = "Collega il caricatore sul telefono",
                    style = TextStyle(
                        color = mutedColor,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                BasicText(
                    text = signedWatt(snapshot.powerW) + " W",
                    style = TextStyle(
                        color = accent,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                BasicText(
                    text = "${snapshot.levelPercent}%",
                    style = TextStyle(
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                )
                val subtitle = snapshot.etaLabel.ifEmpty {
                    snapshot.statusLabel.ifEmpty { "\u2014" }
                }
                BasicText(
                    text = subtitle,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(top = 6.dp)
                )
                BasicText(
                    text = freshnessLabel(secondsAgo),
                    style = TextStyle(
                        color = mutedColor,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private fun signedWatt(value: Float): String {
    val magnitude = abs(value)
    val body = if (magnitude >= 10f) {
        String.format(java.util.Locale.ITALY, "%.0f", magnitude)
    } else {
        String.format(java.util.Locale.ITALY, "%.1f", magnitude)
    }
    return when {
        magnitude < 0.05f -> "0"
        value > 0f -> "+$body"
        else -> "-$body"
    }
}

private fun freshnessLabel(secondsAgo: Long): String = when {
    secondsAgo < 8L -> "aggiornato ora"
    secondsAgo < 60L -> "aggiornato ${secondsAgo}s fa"
    secondsAgo < 3600L -> "aggiornato ${secondsAgo / 60L}min fa"
    else -> "dati non recenti"
}
