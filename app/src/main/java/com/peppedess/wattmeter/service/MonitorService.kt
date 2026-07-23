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
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.peppedess.wattmeter.MainActivity
import com.peppedess.wattmeter.R
import com.peppedess.wattmeter.battery.BatteryMonitor
import com.peppedess.wattmeter.battery.ChargeEstimator
import com.peppedess.wattmeter.battery.Format
import com.peppedess.wattmeter.battery.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tiene aggiornata una notifica con potenza, corrente e tempo residuo
 * anche quando l'app non è in primo piano.
 */
class MonitorService : LifecycleService() {

    private lateinit var monitor: BatteryMonitor
    private lateinit var prefs: Prefs
    private var running = false

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
            startForegroundCompat(buildNotification("Avvio del monitoraggio…", "", 0))
            lifecycleScope.launch {
                while (isActive) {
                    val reading = monitor.read(prefs.currentUnit)
                    prefs.updateRecords(reading)
                    val estimate = ChargeEstimator.estimate(reading)

                    val title = if (reading.isCharging) {
                        "${Format.watt(reading.powerW)} W · ${reading.levelPercent}%"
                    } else {
                        "${reading.levelPercent}% · ${reading.statusLabel}"
                    }

                    val text = buildString {
                        append(Format.milliAmp(reading.absCurrentMa))
                        append(" · ")
                        append(Format.volt(reading.voltageV))
                        append(" · ")
                        append(Format.celsius(reading.temperatureC))
                        if (estimate.toFullMs != null) {
                            append("\nPieno tra ")
                            append(Format.duration(estimate.toFullMs))
                            append(" (")
                            append(Format.clockTime(reading.timestamp, estimate.toFullMs))
                            append(")")
                        }
                    }

                    notify(buildNotification(title, text, reading.levelPercent))
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

    private fun buildNotification(title: String, text: String, progress: Int): Notification {
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text.replace("\n", " · "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .addAction(0, "Ferma", stopIntent)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Monitoraggio ricarica",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifica con potenza e tempo residuo di ricarica"
            setShowBadge(false)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "wattmeter_live"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.peppedess.wattmeter.STOP"
        private const val UPDATE_INTERVAL_MS = 2000L

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
