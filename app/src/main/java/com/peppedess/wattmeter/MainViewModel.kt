package com.peppedess.wattmeter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.peppedess.wattmeter.battery.BatteryMonitor
import com.peppedess.wattmeter.battery.BatteryReading
import com.peppedess.wattmeter.battery.ChargeEstimate
import com.peppedess.wattmeter.battery.ChargeEstimator
import com.peppedess.wattmeter.battery.CurrentUnit
import com.peppedess.wattmeter.battery.Alerts
import com.peppedess.wattmeter.battery.HistoryStats
import com.peppedess.wattmeter.battery.Prefs
import com.peppedess.wattmeter.battery.SessionHistory
import com.peppedess.wattmeter.battery.SessionRecord
import com.peppedess.wattmeter.battery.Reactivity
import com.peppedess.wattmeter.battery.SessionStats
import com.peppedess.wattmeter.battery.SessionTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val reading: BatteryReading = BatteryReading.EMPTY,
    val estimate: ChargeEstimate = ChargeEstimate(null, null, null, null, "—"),
    val session: SessionStats = SessionStats(),
    val history: List<Float> = emptyList(),
    val currentUnit: CurrentUnit = CurrentUnit.AUTO,
    val onlyWhileCharging: Boolean = true,
    val dynamicColor: Boolean = false,
    val reactivity: Reactivity = Reactivity.BALANCED,
    val records: List<SessionRecord> = emptyList(),
    val historyStats: HistoryStats = HistoryStats(),
    val alertLevelEnabled: Boolean = false,
    val alertLevel: Int = 80,
    val alertTemperatureEnabled: Boolean = false,
    val alertTemperature: Int = 42,
    val recordPowerW: Float = 0f,
    val recordCurrentMa: Float = 0f,
    val serviceRunning: Boolean = false,
    val ready: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val monitor = BatteryMonitor(app)
    private val tracker = SessionTracker(app)
    private val prefs = Prefs(app)
    private val historyStore = SessionHistory(app)
    private val alerts = Alerts(app)

    private val history = ArrayDeque<Float>()

    private val _state = MutableStateFlow(
        UiState(
            currentUnit = prefs.currentUnit,
            onlyWhileCharging = prefs.onlyWhileCharging,
            dynamicColor = prefs.dynamicColor,
            reactivity = prefs.reactivity,
            recordPowerW = prefs.recordPowerW,
            recordCurrentMa = prefs.recordCurrentMa,
            alertLevelEnabled = prefs.alertLevelEnabled,
            alertLevel = prefs.alertLevel,
            alertTemperatureEnabled = prefs.alertTemperatureEnabled,
            alertTemperature = prefs.alertTemperature
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refreshHistory()
        viewModelScope.launch {
            monitor.readings(
                unit = { prefs.currentUnit },
                reactivity = { prefs.reactivity }
            ).collect { reading -> onReading(reading) }
        }
    }

    private fun onReading(reading: BatteryReading) {
        val wasActive = _state.value.session.active
        val session = tracker.update(reading)
        prefs.updateRecords(reading)
        alerts.check(reading, session.startTime)

        // Ricarica appena conclusa: l'archivio ha una voce in piu da mostrare
        if (wasActive && !session.active) {
            refreshHistory()
        }

        history.addLast(reading.signedPowerW)
        while (history.size > HISTORY_SIZE) {
            history.removeFirst()
        }

        _state.value = _state.value.copy(
            reading = reading,
            estimate = ChargeEstimator.estimate(reading),
            session = session,
            history = history.toList(),
            recordPowerW = prefs.recordPowerW,
            recordCurrentMa = prefs.recordCurrentMa,
            ready = true
        )
    }

    fun setCurrentUnit(unit: CurrentUnit) {
        prefs.currentUnit = unit
        _state.value = _state.value.copy(currentUnit = unit)
    }

    fun setOnlyWhileCharging(enabled: Boolean) {
        prefs.onlyWhileCharging = enabled
        _state.value = _state.value.copy(onlyWhileCharging = enabled)
    }

    fun setReactivity(value: Reactivity) {
        prefs.reactivity = value
        _state.value = _state.value.copy(reactivity = value)
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.dynamicColor = enabled
        _state.value = _state.value.copy(dynamicColor = enabled)
    }

    fun setServiceRunning(running: Boolean) {
        _state.value = _state.value.copy(serviceRunning = running)
    }

    fun resetRecords() {
        prefs.resetRecords()
        _state.value = _state.value.copy(recordPowerW = 0f, recordCurrentMa = 0f)
    }

    fun resetSession() {
        tracker.reset()
        history.clear()
        _state.value = _state.value.copy(session = SessionStats(), history = emptyList())
    }

    fun refreshHistory() {
        val records = historyStore.load()
        _state.value = _state.value.copy(
            records = records,
            historyStats = historyStore.stats(records)
        )
    }

    fun clearHistory() {
        historyStore.clear()
        _state.value = _state.value.copy(records = emptyList(), historyStats = HistoryStats())
    }

    fun setAlertLevelEnabled(enabled: Boolean) {
        prefs.alertLevelEnabled = enabled
        _state.value = _state.value.copy(alertLevelEnabled = enabled)
    }

    fun setAlertLevel(value: Int) {
        prefs.alertLevel = value
        _state.value = _state.value.copy(alertLevel = prefs.alertLevel)
    }

    fun setAlertTemperatureEnabled(enabled: Boolean) {
        prefs.alertTemperatureEnabled = enabled
        _state.value = _state.value.copy(alertTemperatureEnabled = enabled)
    }

    fun setAlertTemperature(value: Int) {
        prefs.alertTemperature = value
        _state.value = _state.value.copy(alertTemperature = prefs.alertTemperature)
    }

    companion object {
        private const val HISTORY_SIZE = 180
    }
}
