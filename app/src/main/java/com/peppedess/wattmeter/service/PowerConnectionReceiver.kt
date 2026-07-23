package com.peppedess.wattmeter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.peppedess.wattmeter.battery.Prefs

/**
 * Avvia o ferma la notifica live quando il caricatore viene collegato o scollegato,
 * ma solo se l'utente ha attivato l'opzione nelle impostazioni.
 */
class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs(context)
        if (!prefs.autoStartOnPlug) return

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> MonitorService.start(context)
            Intent.ACTION_POWER_DISCONNECTED -> MonitorService.stop(context)
        }
    }
}
