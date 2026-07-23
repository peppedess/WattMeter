package com.peppedess.wattmeter.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.abs

/**
 * Legge lo stato della batteria unendo due sorgenti:
 * - lo sticky broadcast ACTION_BATTERY_CHANGED (livello, tensione, temperatura, stato)
 * - BatteryManager (corrente istantanea, carica residua, salute, cicli)
 *
 * Nessun permesso richiesto: sono tutti dati pubblici del framework.
 */
class BatteryMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val batteryManager =
        appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private val currentWindow = ArrayDeque<Float>()
    private var lastFullCapacity: Float? = null

    /** Emette una lettura ogni [intervalMs] millisecondi. */
    fun readings(intervalMs: Long, unit: () -> CurrentUnit): Flow<BatteryReading> = flow {
        while (true) {
            emit(read(unit()))
            delay(intervalMs)
        }
    }

    fun read(unit: CurrentUnit = CurrentUnit.AUTO): BatteryReading {
        val intent: Intent? = appContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val rawLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val level = if (rawLevel >= 0 && scale > 0) {
            (rawLevel * 100f / scale).toInt().coerceIn(0, 100)
        } else {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                .coerceIn(0, 100)
        }

        val status = intent?.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN
        ) ?: BatteryManager.BATTERY_STATUS_UNKNOWN

        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val health = intent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN
        ) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN

        val technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "—"

        val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperatureC = tempRaw / 10f

        val voltageRaw = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltageV = normalizeVoltage(voltageRaw)

        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                (plugged != 0 && status != BatteryManager.BATTERY_STATUS_DISCHARGING)

        val currentMa = normalizeCurrent(
            raw = safeProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
            unit = unit,
            charging = charging
        )

        val averageRaw = safeProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val hardwareAverage = if (averageRaw != null) {
            normalizeCurrent(averageRaw, unit, charging)
        } else {
            null
        }

        pushCurrent(currentMa)
        val averageCurrentMa = hardwareAverage ?: movingAverage()

        val powerW = abs(currentMa) / 1000f * voltageV

        val chargeCounterRaw = safeProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val chargeCounterMah = chargeCounterRaw?.let { normalizeChargeCounter(it) }

        val fullCapacityMah = estimateFullCapacity(chargeCounterMah, level)

        val stateOfHealth = if (Build.VERSION.SDK_INT >= 35) {
            safeProperty(BatteryManager.BATTERY_PROPERTY_STATE_OF_HEALTH)
                ?.takeIf { it in 1..100 }
        } else {
            null
        }

        val cycleCount = if (Build.VERSION.SDK_INT >= 34) {
            intent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)?.takeIf { it > 0 }
        } else {
            null
        }

        val systemEta = if (Build.VERSION.SDK_INT >= 28) {
            runCatching { batteryManager.computeChargeTimeRemaining() }
                .getOrNull()
                ?.takeIf { it > 0 }
        } else {
            null
        }

        return BatteryReading(
            timestamp = System.currentTimeMillis(),
            levelPercent = level,
            status = status,
            plugged = plugged,
            health = health,
            technology = technology,
            temperatureC = temperatureC,
            voltageV = voltageV,
            currentMa = currentMa,
            averageCurrentMa = averageCurrentMa,
            powerW = powerW,
            chargeCounterMah = chargeCounterMah,
            fullCapacityMah = fullCapacityMah,
            stateOfHealthPercent = stateOfHealth,
            cycleCount = cycleCount,
            systemEtaMs = systemEta
        )
    }

    private fun safeProperty(id: Int): Int? {
        val value = runCatching { batteryManager.getIntProperty(id) }.getOrNull() ?: return null
        if (value == Int.MIN_VALUE || value == Int.MAX_VALUE) return null
        if (value == 0 && id != BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) return null
        return value
    }

    /**
     * Il framework dichiara microampere, ma diversi OEM (Samsung in testa) restituiscono mA.
     * In modalità automatica si sceglie in base all'ordine di grandezza.
     * Il segno viene allineato allo stato reale: molti dispositivi lo invertono.
     */
    private fun normalizeCurrent(raw: Int?, unit: CurrentUnit, charging: Boolean): Float {
        if (raw == null) return 0f
        val magnitude = when (unit) {
            CurrentUnit.MICRO_AMP -> abs(raw) / 1000f
            CurrentUnit.MILLI_AMP -> abs(raw).toFloat()
            CurrentUnit.AUTO -> if (abs(raw) > 20_000) abs(raw) / 1000f else abs(raw).toFloat()
        }
        if (magnitude > 25_000f) return 0f
        return if (charging) magnitude else -magnitude
    }

    private fun normalizeVoltage(raw: Int): Float = when {
        raw <= 0 -> 0f
        raw > 1000 -> raw / 1000f
        raw > 100 -> raw / 100f
        else -> raw.toFloat()
    }

    /** CHARGE_COUNTER è in µAh sulla quasi totalità dei dispositivi, ma alcuni usano mAh. */
    private fun normalizeChargeCounter(raw: Int): Float {
        val value = abs(raw)
        return if (value > 50_000) value / 1000f else value.toFloat()
    }

    private fun estimateFullCapacity(chargeCounterMah: Float?, level: Int): Float? {
        if (chargeCounterMah == null || level <= 5) return lastFullCapacity
        val estimate = chargeCounterMah * 100f / level
        if (estimate < 500f || estimate > 20_000f) return lastFullCapacity
        val previous = lastFullCapacity
        val smoothed = if (previous == null) estimate else previous * 0.9f + estimate * 0.1f
        lastFullCapacity = smoothed
        return smoothed
    }

    private fun pushCurrent(value: Float) {
        currentWindow.addLast(value)
        while (currentWindow.size > WINDOW_SIZE) {
            currentWindow.removeFirst()
        }
    }

    private fun movingAverage(): Float {
        if (currentWindow.isEmpty()) return 0f
        return currentWindow.sum() / currentWindow.size
    }

    companion object {
        private const val WINDOW_SIZE = 20
    }
}
