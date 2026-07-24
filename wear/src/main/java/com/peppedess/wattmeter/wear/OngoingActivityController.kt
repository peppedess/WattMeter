package com.peppedess.wattmeter.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import kotlin.math.abs

/**
 * Mostra l'icona a fulmine sul quadrante e nei Recenti mentre il telefono
 * e in carica, sparisce da sola quando il telefono manda charging=false.
 */
object OngoingActivityController {

    private const val CHANNEL_ID = "wattmeter_watch_live"
    private const val NOTIFICATION_ID = 4001

    fun update(context: Context, snapshot: WatchSnapshot) {
        ensureChannel(context)

        if (!snapshot.charging) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return
        }

        val touchIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "${signedWatt(snapshot.powerW)} W \u00b7 ${snapshot.levelPercent}%"
        val subtitle = snapshot.etaLabel.ifEmpty { snapshot.statusLabel }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_bolt)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(touchIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)

        val status = Status.Builder()
            .addTemplate("#status#")
            .addPart("status", Status.TextPart(title))
            .build()

        val existing = OngoingActivity.recoverOngoingActivity(context)
        if (existing != null) {
            existing.update(context, status)
        } else {
            val ongoingActivity = OngoingActivity.Builder(context, NOTIFICATION_ID, notificationBuilder)
                .setStaticIcon(R.drawable.ic_bolt)
                .setTouchIntent(touchIntent)
                .setStatus(status)
                .build()
            ongoingActivity.apply(context)
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ricarica in corso",
            NotificationManager.IMPORTANCE_LOW
        )
        runCatching { manager.createNotificationChannel(channel) }
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
}
