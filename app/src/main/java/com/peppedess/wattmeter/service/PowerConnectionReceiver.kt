package com.peppedess.wattmeter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.peppedess.wattmeter.battery.Prefs

/**
 * Segue il cavo: alla connessione accende la notifica live, allo scollegamento la spegne.
 * Attivo solo quando l'utente ha scelto di monitorare la sola ricarica.
 */
class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs(context).onlyWhileCharging) return

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> MonitorService.start(context)
            Intent.ACTION_POWER_DISCONNECTED -> MonitorService.stop(context)
        }
    }
}
