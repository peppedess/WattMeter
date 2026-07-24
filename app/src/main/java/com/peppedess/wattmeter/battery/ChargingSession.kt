package com.peppedess.wattmeter.battery

import android.content.Context
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Statistiche accumulate dall'istante in cui è stato collegato il caricatore. */
data class SessionStats(
    val active: Boolean = false,
    val startTime: Long = 0L,
    val startLevel: Int = 0,
    val currentLevel: Int = 0,
    val durationMs: Long = 0L,
    val energyWh: Float = 0f,
    val peakPowerW: Float = 0f,
    val averagePowerW: Float = 0f,
    val peakCurrentMa: Float = 0f,
    val maxTemperatureC: Float = 0f,
    val samples: Int = 0
) {
    val gainedPercent: Int get() = currentLevel - startLevel

    /** Velocità media di riempimento espressa in punti percentuali all'ora. */
    val percentPerHour: Float
        get() {
            val hours = durationMs / 3_600_000f
            return if (hours <= 0.01f) 0f else gainedPercent / hours
        }
}

/** Stime di completamento calcolate sulla corrente media reale. */
data class ChargeEstimate(
    val toFullMs: Long?,
    val toEightyMs: Long?,
    val toEmptyMs: Long?,
    val remainingMah: Float?,
    val source: String
)

/**
 * Segue la ricarica in corso e la archivia quando finisce.
 *
 * Lo stato vive nelle preferenze, non in memoria: cosi app e servizio vedono
 * la stessa sessione, che sopravvive alla chiusura dell'app e viene salvata
 * anche se la ricarica termina a schermo spento.
 */
class SessionTracker(context: Context) {

    private val appContext = context.applicationContext
    private val store = appContext.getSharedPreferences("wattmeter_session", Context.MODE_PRIVATE)
    private val history = SessionHistory(appContext)

    fun update(reading: BatteryReading): SessionStats {
        val active = store.getBoolean(K_ACTIVE, false)

        if (!reading.isCharging && !reading.isFull) {
            if (active) {
                archive()
                clear()
            }
            return SessionStats()
        }

        if (!active) {
            begin(reading)
            return current(reading.levelPercent)
        }

        // Se app e servizio girano insieme, il secondo arrivato non deve
        // sommare di nuovo lo stesso intervallo
        val last = store.getLong(K_LAST_TS, 0L)
        if (reading.timestamp - last < MIN_STEP_MS) {
            return current(reading.levelPercent)
        }

        accumulate(reading, last)
        return current(reading.levelPercent)
    }

    fun reset() {
        clear()
    }

    private fun begin(reading: BatteryReading) {
        store.edit()
            .putBoolean(K_ACTIVE, true)
            .putLong(K_START, reading.timestamp)
            .putInt(K_START_LEVEL, reading.levelPercent)
            .putLong(K_LAST_TS, reading.timestamp)
            .putFloat(K_ENERGY, 0f)
            .putFloat(K_PEAK_POWER, reading.powerW)
            .putFloat(K_PEAK_CURRENT, reading.absCurrentMa)
            .putFloat(K_MAX_TEMP, reading.temperatureC)
            .putFloat(K_POWER_SUM, reading.powerW)
            .putInt(K_SAMPLES, 1)
            .putString(K_SOURCE, reading.sourceLabel)
            .apply()
    }

    private fun accumulate(reading: BatteryReading, last: Long) {
        val deltaMs = (reading.timestamp - last).coerceIn(0L, 60_000L)
        val deltaHours = deltaMs / 3_600_000f
        val samples = store.getInt(K_SAMPLES, 0) + 1

        store.edit()
            .putLong(K_LAST_TS, reading.timestamp)
            .putFloat(K_ENERGY, store.getFloat(K_ENERGY, 0f) + reading.powerW * deltaHours)
            .putFloat(K_PEAK_POWER, max(store.getFloat(K_PEAK_POWER, 0f), reading.powerW))
            .putFloat(K_PEAK_CURRENT, max(store.getFloat(K_PEAK_CURRENT, 0f), reading.absCurrentMa))
            .putFloat(K_MAX_TEMP, max(store.getFloat(K_MAX_TEMP, 0f), reading.temperatureC))
            .putFloat(K_POWER_SUM, store.getFloat(K_POWER_SUM, 0f) + reading.powerW)
            .putInt(K_SAMPLES, samples)
            .putInt(K_LEVEL, reading.levelPercent)
            .apply()
    }

    private fun current(level: Int): SessionStats {
        val start = store.getLong(K_START, 0L)
        val samples = store.getInt(K_SAMPLES, 0).coerceAtLeast(1)
        return SessionStats(
            active = true,
            startTime = start,
            startLevel = store.getInt(K_START_LEVEL, level),
            currentLevel = level,
            durationMs = (store.getLong(K_LAST_TS, start) - start).coerceAtLeast(0L),
            energyWh = store.getFloat(K_ENERGY, 0f),
            peakPowerW = store.getFloat(K_PEAK_POWER, 0f),
            averagePowerW = store.getFloat(K_POWER_SUM, 0f) / samples,
            peakCurrentMa = store.getFloat(K_PEAK_CURRENT, 0f),
            maxTemperatureC = store.getFloat(K_MAX_TEMP, 0f),
            samples = samples
        )
    }

    private fun archive() {
        val start = store.getLong(K_START, 0L)
        val end = store.getLong(K_LAST_TS, 0L)
        if (start <= 0L || end <= start) return

        val samples = store.getInt(K_SAMPLES, 0).coerceAtLeast(1)
        history.add(
            SessionRecord(
                startTime = start,
                endTime = end,
                startLevel = store.getInt(K_START_LEVEL, 0),
                endLevel = store.getInt(K_LEVEL, store.getInt(K_START_LEVEL, 0)),
                energyWh = store.getFloat(K_ENERGY, 0f),
                peakPowerW = store.getFloat(K_PEAK_POWER, 0f),
                averagePowerW = store.getFloat(K_POWER_SUM, 0f) / samples,
                maxTemperatureC = store.getFloat(K_MAX_TEMP, 0f),
                source = store.getString(K_SOURCE, "—") ?: "—"
            )
        )
    }

    private fun clear() {
        store.edit().clear().apply()
    }

    companion object {
        private const val MIN_STEP_MS = 900L
        private const val K_ACTIVE = "active"
        private const val K_START = "start"
        private const val K_START_LEVEL = "start_level"
        private const val K_LEVEL = "level"
        private const val K_LAST_TS = "last_ts"
        private const val K_ENERGY = "energy"
        private const val K_PEAK_POWER = "peak_power"
        private const val K_PEAK_CURRENT = "peak_current"
        private const val K_MAX_TEMP = "max_temp"
        private const val K_POWER_SUM = "power_sum"
        private const val K_SAMPLES = "samples"
        private const val K_SOURCE = "source"
    }
}

object ChargeEstimator {

    /**
     * Sopra l'80% quasi tutti i dispositivi passano a corrente costante decrescente:
     * l'ultimo tratto viene quindi pesato con un fattore di rallentamento.
     */
    private const val TRICKLE_FACTOR = 0.45f
    private const val TRICKLE_THRESHOLD = 80

    fun estimate(reading: BatteryReading): ChargeEstimate {
        if (reading.isFull) {
            return ChargeEstimate(null, null, null, null, "—")
        }

        if (!reading.isCharging) {
            return ChargeEstimate(
                toFullMs = null,
                toEightyMs = null,
                toEmptyMs = estimateAutonomy(reading),
                remainingMah = reading.chargeCounterMah,
                source = "Autonomia sul consumo medio"
            )
        }

        val capacity = reading.fullCapacityMah
        val current = abs(reading.averageCurrentMa).takeIf { it > 30f }
            ?: abs(reading.currentMa).takeIf { it > 30f }

        if (capacity != null && current != null) {
            val remaining = capacity * (100 - reading.levelPercent) / 100f
            val toFull = weightedMinutes(reading.levelPercent, 100, capacity, current)
            val toEighty = if (reading.levelPercent < TRICKLE_THRESHOLD) {
                weightedMinutes(reading.levelPercent, TRICKLE_THRESHOLD, capacity, current)
            } else {
                null
            }
            return ChargeEstimate(
                toFullMs = (toFull * 60_000L),
                toEightyMs = toEighty?.let { it * 60_000L },
                toEmptyMs = null,
                remainingMah = remaining,
                source = "Calcolata sulla corrente media"
            )
        }

        reading.systemEtaMs?.let {
            return ChargeEstimate(it, null, null, null, "Fornita dal sistema")
        }

        return ChargeEstimate(null, null, null, null, "Dati insufficienti")
    }

    /** Tempo residuo a batteria, sul consumo medio recente. */
    private fun estimateAutonomy(reading: BatteryReading): Long? {
        val charge = reading.chargeCounterMah ?: return null
        val drain = abs(reading.averageCurrentMa).takeIf { it > 20f }
            ?: abs(reading.currentMa).takeIf { it > 20f }
            ?: return null
        val hours = charge / drain
        if (hours <= 0f || hours > 200f) return null
        return (hours * 3_600_000f).toLong()
    }

    private fun weightedMinutes(
        fromLevel: Int,
        toLevel: Int,
        capacityMah: Float,
        currentMa: Float
    ): Long {
        if (toLevel <= fromLevel) return 0L
        val fastEnd = minOf(toLevel, TRICKLE_THRESHOLD)
        val fastPercent = max(0, fastEnd - fromLevel)
        val slowPercent = max(0, toLevel - max(fromLevel, TRICKLE_THRESHOLD))

        val fastMah = capacityMah * fastPercent / 100f
        val slowMah = capacityMah * slowPercent / 100f

        val minutes = (fastMah / currentMa) * 60f +
                (slowMah / (currentMa * TRICKLE_FACTOR)) * 60f
        return minutes.roundToInt().toLong().coerceIn(0L, 60L * 48L)
    }
}
