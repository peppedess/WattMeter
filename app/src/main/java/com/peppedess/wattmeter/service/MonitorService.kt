package com.peppedess.wattmeter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.peppedess.wattmeter.MainActivity
import com.peppedess.wattmeter.R
import com.peppedess.wattmeter.battery.BatteryMonitor
import com.peppedess.wattmeter.battery.BatteryReading
import com.peppedess.wattmeter.battery.ChargeEstimate
import com.peppedess.wattmeter.battery.ChargeEstimator
import com.peppedess.wattmeter.battery.Format
import com.peppedess.wattmeter.battery.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Notifica sempre aggiornata con potenza, corrente e tempo residuo.
 *
 * Usa le Live Updates di Android 16 (ProgressStyle + promozione), che One UI 8
 * riprende nella Now Bar. Sui dispositivi più vecchi degrada da sola a una
 * normale notifica con barra di avanzamento.
 */
class MonitorService : LifecycleService() {

    private lateinit var monitor: BatteryMonitor
    private lateinit var prefs: Prefs
    private var running = false
    private var wasCharging = false

    override fun onCreate() {
        super.onCreate()
        monitor = BatteryMonitor(this)
        prefs = Prefs(this)
        createChannel()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!running) {
            running = true
            val first = monitor.read(prefs.currentUnit)
            wasCharging = first.isCharging
            startForegroundCompat(build(first, ChargeEstimator.estimate(first)))
            lifecycleScope.launch {
                while (isActive) {
                    val reading = monitor.read(prefs.currentUnit)
                    prefs.updateRecords(reading)

                    // Cavo staccato: se richiesto, sparisce tutto invece di restare appesa
                    if (wasCharging && !reading.isCharging && prefs.onlyWhileCharging) {
                        stopSelf()
                        return@launch
                    }
                    wasCharging = reading.isCharging

                    notify(build(reading, ChargeEstimator.estimate(reading)))
                    delay(UPDATE_INTERVAL_MS)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    private fun build(reading: BatteryReading, estimate: ChargeEstimate): Notification {
        val charging = reading.isCharging
        val level = reading.levelPercent.coerceIn(0, 100)

        val title = if (charging) {
            "${Format.signedWatt(reading.signedPowerW)} W · $level%"
        } else {
            "$level% · ${Format.milliAmp(reading.absCurrentMa)}"
        }

        val body = buildString {
            when {
                estimate.toFullMs != null -> {
                    append("Pieno alle ")
                    append(Format.clockTime(reading.timestamp, estimate.toFullMs))
                    append(" · fra ")
                    append(Format.duration(estimate.toFullMs))
                }
                estimate.toEmptyMs != null -> {
                    append("Autonomia ")
                    append(Format.duration(estimate.toEmptyMs))
                }
                reading.isFull -> append("Carica completa")
                else -> append(reading.statusLabel)
            }
            append("  ·  ")
            append(Format.volt(reading.voltageV))
            append("  ·  ")
            append(Format.celsius(reading.temperatureC))
        }

        // Testo brevissimo mostrato nel chip della barra di stato
        val chip = if (charging) {
            val w = abs(reading.powerW)
            if (w >= 10f) "${w.roundToInt()}W" else "${Format.watt(w)}W"
        } else {
            "$level%"
        }

        // Barra a due tratti: verde fino all'80%, ambra nel tratto finale piu lento
        val style = if (charging) {
            NotificationCompat.ProgressStyle()
                .setProgressSegments(
                    listOf(
                        NotificationCompat.ProgressStyle.Segment(80).setColor(COLOR_FAST),
                        NotificationCompat.ProgressStyle.Segment(20).setColor(COLOR_TRICKLE)
                    )
                )
                .setProgress(level)
                .setProgressTrackerIcon(
                    IconCompat.createWithResource(this, R.drawable.ic_notification)
                )
        } else {
            null
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .addAction(0, "Ferma", stopIntent)

        if (charging && style != null) {
            // Live Update: chip in barra di stato, Now Bar su One UI 8, barra di avanzamento
            builder.setStyle(style)
                .setRequestPromotedOngoing(true)
                .setShortCriticalText(chip)
        } else {
            // A batteria resta solo una riga discreta nella tendina
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setRequestPromotedOngoing(false)
                .setSilent(true)
        }

        return builder.build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notify(notification: Notification) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ricarica live",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Potenza, tempo residuo e avanzamento della ricarica in tempo reale"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "wattmeter_live"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.peppedess.wattmeter.STOP"
        private const val UPDATE_INTERVAL_MS = 2000L

        private val COLOR_FAST = 0xFF00A050.toInt()
        private val COLOR_TRICKLE = 0xFFE07B00.toInt()

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, MonitorService::class.java))
            }
        }

        fun isRunning(context: Context): Boolean {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return manager.activeNotifications.any { it.id == NOTIFICATION_ID }
        }
    }
}
