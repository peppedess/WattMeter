package com.peppedess.wattmeter.battery

import android.os.BatteryManager
import kotlin.math.abs

/**
 * Fotografia istantanea dello stato della batteria.
 * Le correnti sono sempre normalizzate in mA con segno:
 * positivo = energia in ingresso (ricarica), negativo = energia in uscita (scarica).
 */
data class BatteryReading(
    val timestamp: Long,
    val levelPercent: Int,
    val status: Int,
    val plugged: Int,
    val health: Int,
    val technology: String,
    val temperatureC: Float,
    val voltageV: Float,
    val currentMa: Float,
    val averageCurrentMa: Float,
    val powerW: Float,
    val chargeCounterMah: Float?,
    val fullCapacityMah: Float?,
    val stateOfHealthPercent: Int?,
    val cycleCount: Int?,
    val systemEtaMs: Long?
) {
    val isCharging: Boolean
        get() = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                (plugged != 0 && status != BatteryManager.BATTERY_STATUS_DISCHARGING)

    val isFull: Boolean
        get() = status == BatteryManager.BATTERY_STATUS_FULL

    val absCurrentMa: Float
        get() = abs(currentMa)

    val sourceLabel: String
        get() = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Alimentatore AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            4 -> "Dock"
            else -> "Non collegato"
        }

    val healthLabel: String
        get() = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Buona"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Surriscaldata"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Esausta"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sovratensione"
            BatteryManager.BATTERY_HEALTH_COLD -> "Troppo fredda"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Guasto"
            else -> "Sconosciuta"
        }

    val statusLabel: String
        get() = when {
            isFull -> "Carica completa"
            isCharging -> "In ricarica"
            status == BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Collegato, non in carica"
            else -> "In scarica"
        }

    /** Classificazione della velocità di ricarica in base alla potenza reale in ingresso. */
    val speedLabel: String
        get() = when {
            !isCharging -> "—"
            powerW >= 40f -> "Ultra rapida"
            powerW >= 20f -> "Molto rapida"
            powerW >= 12f -> "Rapida"
            powerW >= 6f -> "Standard"
            powerW >= 2f -> "Lenta"
            else -> "Molto lenta"
        }

    companion object {
        val EMPTY = BatteryReading(
            timestamp = 0L,
            levelPercent = 0,
            status = BatteryManager.BATTERY_STATUS_UNKNOWN,
            plugged = 0,
            health = BatteryManager.BATTERY_HEALTH_UNKNOWN,
            technology = "—",
            temperatureC = 0f,
            voltageV = 0f,
            currentMa = 0f,
            averageCurrentMa = 0f,
            powerW = 0f,
            chargeCounterMah = null,
            fullCapacityMah = null,
            stateOfHealthPercent = null,
            cycleCount = null,
            systemEtaMs = null
        )
    }
}

/** Come interpretare il valore grezzo restituito da BATTERY_PROPERTY_CURRENT_NOW. */
enum class CurrentUnit(val label: String) {
    AUTO("Automatica"),
    MICRO_AMP("Microampere (µA)"),
    MILLI_AMP("Milliampere (mA)")
}
