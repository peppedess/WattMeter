package com.peppedess.wattmeter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.peppedess.wattmeter.battery.Prefs
import com.peppedess.wattmeter.widget.WattWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Segue il cavo: alla connessione accende la notifica live, allo scollegamento la spegne.
 * Attivo solo quando l'utente ha scelto di monitorare la sola ricarica.
 */
class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_POWER_CONNECTED &&
            intent.action != Intent.ACTION_POWER_DISCONNECTED
        ) {
            return
        }

        // Il widget deve reagire al cavo anche quando il servizio non parte
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WattWidget().updateAll(context.applicationContext)
            } finally {
                pending.finish()
            }
        }

        if (!Prefs(context).onlyWhileCharging) return

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> MonitorService.start(context)
            Intent.ACTION_POWER_DISCONNECTED -> MonitorService.stop(context)
        }
    }
}
