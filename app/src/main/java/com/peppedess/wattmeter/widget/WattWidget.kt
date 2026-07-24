package com.peppedess.wattmeter.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.peppedess.wattmeter.MainActivity
import com.peppedess.wattmeter.battery.BatteryMonitor
import com.peppedess.wattmeter.battery.BatteryReading
import com.peppedess.wattmeter.battery.ChargeEstimate
import com.peppedess.wattmeter.battery.ChargeEstimator
import com.peppedess.wattmeter.battery.Format
import com.peppedess.wattmeter.battery.Prefs

private val Verde = Color(0xFF00A050)
private val VerdeChiaro = Color(0xFFB9F7CF)
private val Ambra = Color(0xFFE07B00)
private val AmbraChiaro = Color(0xFFFFE2B8)
private val Scuro = Color(0xFF0E1F14)

class WattWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val reading = BatteryMonitor(context).read(Prefs(context).currentUnit, 1f)
        val estimate = ChargeEstimator.estimate(reading)

        provideContent {
            WidgetBody(reading, estimate)
        }
    }
}

@Composable
private fun WidgetBody(reading: BatteryReading, estimate: ChargeEstimate) {
    val charging = reading.isCharging
    val accent = if (charging) Verde else Ambra
    val sfondo = if (charging) VerdeChiaro else AmbraChiaro

    val sotto = when {
        estimate.toFullMs != null -> "pieno tra ${Format.duration(estimate.toFullMs)}"
        estimate.toEmptyMs != null -> "autonomia ${Format.duration(estimate.toEmptyMs)}"
        reading.isFull -> "carica completa"
        else -> reading.sourceLabel.lowercase()
    }

    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(sfondo)
            .cornerRadius(24.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(
                actionStartActivity(
                    Intent(context, MainActivity::class.java)
                )
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = Format.signedWatt(reading.signedPowerW),
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(accent)
                )
            )
            Text(
                text = " W",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(accent)
                )
            )
            Text(
                text = "   ${reading.levelPercent}%",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Scuro)
                )
            )
        }
        Text(
            text = sotto,
            style = TextStyle(
                fontSize = 13.sp,
                color = ColorProvider(Scuro)
            )
        )
    }
}

class WattWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WattWidget()
}
