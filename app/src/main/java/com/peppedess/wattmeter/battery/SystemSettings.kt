package com.peppedess.wattmeter.battery

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Scorciatoie verso le schermate di sistema che governano le notifiche.
 *
 * L'interruttore degli aggiornamenti live e per singola app e l'utente puo
 * revocarlo in qualsiasi momento: senza quello la notifica resta una normale
 * riga nella tendina, senza chip ne spazio dedicato in schermata di blocco.
 */
object SystemSettings {

    /** Schermata dedicata agli aggiornamenti live, con ripiego sulle notifiche dell'app. */
    fun openLiveUpdates(context: Context) {
        val action = runCatching {
            Settings::class.java
                .getField("ACTION_MANAGE_APP_PROMOTED_NOTIFICATIONS")
                .get(null) as String
        }.getOrNull() ?: "android.settings.MANAGE_APP_PROMOTED_NOTIFICATIONS"

        val promoted = Intent(action)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (runCatching { context.startActivity(promoted) }.isSuccess) return

        openAppNotifications(context)
    }

    /** Impostazioni notifiche dell'app, disponibili su qualsiasi versione supportata. */
    fun openAppNotifications(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
