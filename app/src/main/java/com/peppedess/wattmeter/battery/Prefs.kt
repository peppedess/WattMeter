package com.peppedess.wattmeter.battery

import android.content.Context
import java.util.Locale
import kotlin.math.abs

class Prefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("wattmeter", Context.MODE_PRIVATE)

    var currentUnit: CurrentUnit
        get() = runCatching {
            CurrentUnit.valueOf(prefs.getString(KEY_UNIT, CurrentUnit.AUTO.name)!!)
        }.getOrDefault(CurrentUnit.AUTO)
        set(value) = prefs.edit().putString(KEY_UNIT, value.name).apply()

    /** La notifica compare al collegamento del caricatore e sparisce quando lo togli. */
    var onlyWhileCharging: Boolean
        get() = prefs.getBoolean(KEY_ONLY_CHARGING, true)
        set(value) = prefs.edit().putBoolean(KEY_ONLY_CHARGING, value).apply()

    var recordPowerW: Float
        get() = prefs.getFloat(KEY_RECORD_POWER, 0f)
        set(value) = prefs.edit().putFloat(KEY_RECORD_POWER, value).apply()

    var recordCurrentMa: Float
        get() = prefs.getFloat(KEY_RECORD_CURRENT, 0f)
        set(value) = prefs.edit().putFloat(KEY_RECORD_CURRENT, value).apply()

    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC, false)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC, value).apply()

    var refreshMs: Long
        get() = prefs.getLong(KEY_REFRESH, 1000L)
        set(value) = prefs.edit().putLong(KEY_REFRESH, value).apply()

    fun updateRecords(reading: BatteryReading) {
        if (!reading.isCharging) return
        if (reading.powerW > recordPowerW && reading.powerW < 300f) {
            recordPowerW = reading.powerW
        }
        if (reading.absCurrentMa > recordCurrentMa && reading.absCurrentMa < 25_000f) {
            recordCurrentMa = reading.absCurrentMa
        }
    }

    fun resetRecords() {
        recordPowerW = 0f
        recordCurrentMa = 0f
    }

    companion object {
        private const val KEY_UNIT = "current_unit"
        private const val KEY_ONLY_CHARGING = "only_while_charging"
        private const val KEY_RECORD_POWER = "record_power"
        private const val KEY_RECORD_CURRENT = "record_current"
        private const val KEY_REFRESH = "refresh_ms"
        private const val KEY_DYNAMIC = "dynamic_color"
    }
}

object Format {

    private val locale = Locale.ITALY

    fun watt(value: Float): String = when {
        abs(value) >= 10f -> String.format(locale, "%.1f", value)
        else -> String.format(locale, "%.2f", value)
    }

    /** Potenza col segno: positiva entra nella batteria, negativa esce. */
    fun signedWatt(value: Float): String {
        val body = watt(abs(value))
        return when {
            abs(value) < 0.005f -> "0"
            value > 0f -> "+$body"
            else -> "-$body"
        }
    }

    /** Numero puro con i decimali richiesti, per le pill che mostrano l'unità a parte. */
    fun number(value: Float, decimals: Int): String =
        String.format(locale, "%.${decimals}f", value)

    fun volt(value: Float): String = String.format(locale, "%.3f V", value)

    fun milliAmp(value: Float): String = String.format(locale, "%.0f mA", value)

    fun celsius(value: Float): String = String.format(locale, "%.1f °C", value)

    fun mah(value: Float?): String =
        if (value == null) "n/d" else String.format(locale, "%.0f mAh", value)

    fun wattHour(value: Float): String = String.format(locale, "%.2f Wh", value)

    fun percentPerHour(value: Float): String = String.format(locale, "%.1f %%/h", value)

    /** Durata leggibile: "1 h 24 min", "47 min", "38 s". */
    fun duration(ms: Long?): String {
        if (ms == null || ms <= 0L) return "—"
        val totalMinutes = ms / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L -> "$hours h $minutes min"
            totalMinutes > 0L -> "$totalMinutes min"
            else -> "${ms / 1000L} s"
        }
    }

    /** Orario previsto di completamento, es. "alle 14:35". */
    fun clockTime(nowMs: Long, deltaMs: Long?): String {
        if (deltaMs == null || deltaMs <= 0L) return "—"
        val target = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMs + deltaMs
        }
        return String.format(
            locale,
            "%02d:%02d",
            target.get(java.util.Calendar.HOUR_OF_DAY),
            target.get(java.util.Calendar.MINUTE)
        )
    }
}
