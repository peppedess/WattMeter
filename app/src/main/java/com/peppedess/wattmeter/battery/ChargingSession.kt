package com.peppedess.wattmeter.battery

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

class SessionTracker {

    private var stats = SessionStats()
    private var lastTimestamp = 0L
    private var powerSum = 0.0

    fun update(reading: BatteryReading): SessionStats {
        if (!reading.isCharging && !reading.isFull) {
            if (stats.active) {
                stats = SessionStats()
                powerSum = 0.0
                lastTimestamp = 0L
            }
            return stats
        }

        if (!stats.active) {
            stats = SessionStats(
                active = true,
                startTime = reading.timestamp,
                startLevel = reading.levelPercent,
                currentLevel = reading.levelPercent,
                maxTemperatureC = reading.temperatureC
            )
            powerSum = 0.0
            lastTimestamp = reading.timestamp
            return stats
        }

        val deltaMs = (reading.timestamp - lastTimestamp).coerceIn(0L, 60_000L)
        lastTimestamp = reading.timestamp

        val deltaHours = deltaMs / 3_600_000f
        powerSum += reading.powerW.toDouble()
        val samples = stats.samples + 1

        stats = stats.copy(
            currentLevel = reading.levelPercent,
            durationMs = reading.timestamp - stats.startTime,
            energyWh = stats.energyWh + reading.powerW * deltaHours,
            peakPowerW = max(stats.peakPowerW, reading.powerW),
            averagePowerW = (powerSum / samples).toFloat(),
            peakCurrentMa = max(stats.peakCurrentMa, reading.absCurrentMa),
            maxTemperatureC = max(stats.maxTemperatureC, reading.temperatureC),
            samples = samples
        )
        return stats
    }

    fun reset() {
        stats = SessionStats()
        powerSum = 0.0
        lastTimestamp = 0L
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
