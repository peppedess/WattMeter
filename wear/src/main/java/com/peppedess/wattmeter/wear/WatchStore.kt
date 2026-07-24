package com.peppedess.wattmeter.wear

import android.content.Context

/** L'ultima fotografia ricevuta dal telefono. */
data class WatchSnapshot(
    val levelPercent: Int = 0,
    val powerW: Float = 0f,
    val statusLabel: String = "",
    val etaLabel: String = "",
    val charging: Boolean = false,
    val timestamp: Long = 0L
)

/**
 * Conserva l'ultima fotografia ricevuta, cosi l'orologio mostra ancora
 * qualcosa di sensato se viene aperto senza che sia appena arrivato un
 * aggiornamento (per esempio subito dopo un riavvio).
 */
class WatchStore private constructor(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("wattmeter_watch", Context.MODE_PRIVATE)

    fun read(): WatchSnapshot = WatchSnapshot(
        levelPercent = prefs.getInt(K_LEVEL, 0),
        powerW = prefs.getFloat(K_POWER, 0f),
        statusLabel = prefs.getString(K_STATUS, "") ?: "",
        etaLabel = prefs.getString(K_ETA, "") ?: "",
        charging = prefs.getBoolean(K_CHARGING, false),
        timestamp = prefs.getLong(K_TIMESTAMP, 0L)
    )

    fun write(snapshot: WatchSnapshot) {
        prefs.edit()
            .putInt(K_LEVEL, snapshot.levelPercent)
            .putFloat(K_POWER, snapshot.powerW)
            .putString(K_STATUS, snapshot.statusLabel)
            .putString(K_ETA, snapshot.etaLabel)
            .putBoolean(K_CHARGING, snapshot.charging)
            .putLong(K_TIMESTAMP, snapshot.timestamp)
            .apply()
    }

    companion object {
        private const val K_LEVEL = "level"
        private const val K_POWER = "power"
        private const val K_STATUS = "status"
        private const val K_ETA = "eta"
        private const val K_CHARGING = "charging"
        private const val K_TIMESTAMP = "timestamp"

        @Volatile private var instance: WatchStore? = null

        fun get(context: Context): WatchStore =
            instance ?: synchronized(this) {
                instance ?: WatchStore(context).also { instance = it }
            }
    }
}
