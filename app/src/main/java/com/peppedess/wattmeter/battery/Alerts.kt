package com.peppedess.wattmeter.battery

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Avvisi durante la ricarica: soglia raggiunta e batteria troppo calda.
 *
 * Ogni avviso scatta una sola volta per sessione, così una carica lunga
 * non si trasforma in una raffica di notifiche.
 */
class Alerts(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = Prefs(appContext)
    private val store =
        appContext.getSharedPreferences("wattmeter_alerts", Context.MODE_PRIVATE)
    private val manager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun check(reading: BatteryReading, sessionStart: Long) {
        if (!reading.isCharging) {
            return
        }
        ensureChannel()

        if (store.getLong(KEY_SESSION, 0L) != sessionStart) {
            store.edit().clear().putLong(KEY_SESSION, sessionStart).apply()
        }

        if (prefs.alertLevelEnabled &&
            reading.levelPercent >= prefs.alertLevel &&
            !store.getBoolean(KEY_LEVEL_DONE, false)
        ) {
            store.edit().putBoolean(KEY_LEVEL_DONE, true).apply()
            send(
                id = ID_LEVEL,
                title = "Batteria al ${reading.levelPercent}%",
                text = "Hai raggiunto la soglia impostata: puoi staccare il caricatore."
            )
        }

        if (prefs.alertTemperatureEnabled &&
            reading.temperatureC >= prefs.alertTemperature &&
            !store.getBoolean(KEY_TEMP_DONE, false)
        ) {
            store.edit().putBoolean(KEY_TEMP_DONE, true).apply()
            send(
                id = ID_TEMP,
                title = "Batteria calda: ${Format.celsius(reading.temperatureC)}",
                text = "Conviene togliere la cover o spostarlo dal sole finché si raffredda."
            )
        }
    }

    private fun send(id: Int, title: String, text: String) {
        val open = PendingIntent.getActivity(
            appContext,
            id,
            appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
                ?: Intent(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = androidx.core.app.NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(com.peppedess.wattmeter.R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        runCatching { manager.notify(id, notification) }
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Avvisi ricarica",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Soglia di carica raggiunta e batteria troppo calda"
            enableVibration(true)
        }
        runCatching { manager.createNotificationChannel(channel) }
    }

    companion object {
        private const val CHANNEL_ID = "wattmeter_alerts"
        private const val ID_LEVEL = 2001
        private const val ID_TEMP = 2002
        private const val KEY_SESSION = "session"
        private const val KEY_LEVEL_DONE = "level_done"
        private const val KEY_TEMP_DONE = "temp_done"
    }
}
