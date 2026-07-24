package com.peppedess.wattmeter.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Riceve ogni aggiornamento che il telefono manda tramite il Data Layer
 * e lo salva, aggiornando anche l'icona sul quadrante se si sta caricando.
 */
class WatchListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == WatchProtocol.PATH }
            .forEach { event ->
                val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                val snapshot = WatchSnapshot(
                    levelPercent = map.getInt(WatchProtocol.KEY_LEVEL),
                    powerW = map.getFloat(WatchProtocol.KEY_POWER),
                    statusLabel = map.getString(WatchProtocol.KEY_STATUS) ?: "",
                    etaLabel = map.getString(WatchProtocol.KEY_ETA) ?: "",
                    charging = map.getBoolean(WatchProtocol.KEY_CHARGING),
                    timestamp = map.getLong(WatchProtocol.KEY_TIMESTAMP)
                )

                WatchStore.get(applicationContext).write(snapshot)
                OngoingActivityController.update(applicationContext, snapshot)
            }
    }
}
