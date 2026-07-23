package com.peppedess.wattmeter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.peppedess.wattmeter.battery.BatteryMonitor
import com.peppedess.wattmeter.battery.BatteryReading
import com.peppedess.wattmeter.battery.ChargeEstimate
import com.peppedess.wattmeter.battery.ChargeEstimator
import com.peppedess.wattmeter.battery.CurrentUnit
import com.peppedess.wattmeter.battery.Prefs
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
    val recordPowerW: Float = 0f,
    val recordCurrentMa: Float = 0f,
    val serviceRunning: Boolean = false,
    val ready: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val monitor = BatteryMonitor(app)
    private val tracker = SessionTracker()
    private val prefs = Prefs(app)

    private val history = ArrayDeque<Float>()

    private val _state = MutableStateFlow(
        UiState(
            currentUnit = prefs.currentUnit,
            onlyWhileCharging = prefs.onlyWhileCharging,
            dynamicColor = prefs.dynamicColor,
            recordPowerW = prefs.recordPowerW,
            recordCurrentMa = prefs.recordCurrentMa
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitor.readings(intervalMs = 1000L, unit = { prefs.currentUnit })
                .collect { reading -> onReading(reading) }
        }
    }

    private fun onReading(reading: BatteryReading) {
        val session = tracker.update(reading)
        prefs.updateRecords(reading)

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

    companion object {
        private const val HISTORY_SIZE = 180
    }
}
