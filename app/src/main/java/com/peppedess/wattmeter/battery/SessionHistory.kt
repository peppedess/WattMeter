package com.peppedess.wattmeter.battery

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Una ricarica conclusa, così come viene archiviata. */
data class SessionRecord(
    val startTime: Long,
    val endTime: Long,
    val startLevel: Int,
    val endLevel: Int,
    val energyWh: Float,
    val peakPowerW: Float,
    val averagePowerW: Float,
    val maxTemperatureC: Float,
    val source: String
) {
    val durationMs: Long get() = (endTime - startTime).coerceAtLeast(0L)
    val gainedPercent: Int get() = (endLevel - startLevel).coerceAtLeast(0)

    val percentPerHour: Float
        get() {
            val hours = durationMs / 3_600_000f
            return if (hours <= 0.02f) 0f else gainedPercent / hours
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("start", startTime)
        put("end", endTime)
        put("lv0", startLevel)
        put("lv1", endLevel)
        put("wh", energyWh.toDouble())
        put("peak", peakPowerW.toDouble())
        put("avg", averagePowerW.toDouble())
        put("temp", maxTemperatureC.toDouble())
        put("src", source)
    }

    companion object {
        fun fromJson(o: JSONObject) = SessionRecord(
            startTime = o.optLong("start"),
            endTime = o.optLong("end"),
            startLevel = o.optInt("lv0"),
            endLevel = o.optInt("lv1"),
            energyWh = o.optDouble("wh", 0.0).toFloat(),
            peakPowerW = o.optDouble("peak", 0.0).toFloat(),
            averagePowerW = o.optDouble("avg", 0.0).toFloat(),
            maxTemperatureC = o.optDouble("temp", 0.0).toFloat(),
            source = o.optString("src", "—")
        )
    }
}

/** Sintesi di tutte le ricariche archiviate. */
data class HistoryStats(
    val count: Int = 0,
    val totalEnergyWh: Float = 0f,
    val averagePowerW: Float = 0f,
    val bestPeakW: Float = 0f,
    val averagePercentPerHour: Float = 0f,
    val averageDurationMs: Long = 0L,
    val maxTemperatureC: Float = 0f
)

/**
 * Archivio su file JSON nella cartella privata dell'app.
 * Nessun database: le ricariche sono poche e la lettura avviene solo su richiesta.
 */
class SessionHistory(context: Context) {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)

    fun load(): List<SessionRecord> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            buildList {
                for (i in 0 until array.length()) {
                    add(SessionRecord.fromJson(array.getJSONObject(i)))
                }
            }.sortedByDescending { it.startTime }
        }.getOrDefault(emptyList())
    }

    /** Archivia una ricarica, ignorando quelle troppo brevi o duplicate. */
    fun add(record: SessionRecord) {
        if (record.durationMs < MIN_DURATION_MS || record.gainedPercent < 1) return

        val current = load().toMutableList()
        val duplicate = current.any { kotlin.math.abs(it.startTime - record.startTime) < 60_000L }
        if (duplicate) return

        current.add(record)
        val trimmed = current.sortedByDescending { it.startTime }.take(MAX_RECORDS)
        save(trimmed)
    }

    fun clear() {
        runCatching { file.delete() }
    }

    fun stats(records: List<SessionRecord> = load()): HistoryStats {
        if (records.isEmpty()) return HistoryStats()
        val valid = records.filter { it.durationMs > 0 }
        return HistoryStats(
            count = records.size,
            totalEnergyWh = records.map { it.energyWh }.sum(),
            averagePowerW = records.map { it.averagePowerW }.average().toFloat(),
            bestPeakW = records.maxOf { it.peakPowerW },
            averagePercentPerHour = if (valid.isEmpty()) {
                0f
            } else {
                valid.map { it.percentPerHour }.average().toFloat()
            },
            averageDurationMs = if (valid.isEmpty()) {
                0L
            } else {
                valid.map { it.durationMs }.average().toLong()
            },
            maxTemperatureC = records.maxOf { it.maxTemperatureC }
        )
    }

    private fun save(records: List<SessionRecord>) {
        runCatching {
            val array = JSONArray()
            records.forEach { array.put(it.toJson()) }
            file.writeText(array.toString())
        }
    }

    companion object {
        private const val FILE_NAME = "sessions.json"
        private const val MAX_RECORDS = 100
        private const val MIN_DURATION_MS = 120_000L
    }
}
