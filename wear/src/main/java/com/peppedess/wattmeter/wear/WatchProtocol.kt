package com.peppedess.wattmeter.wear

/**
 * Chiavi del canale Data Layer fra telefono e orologio.
 *
 * Il telefono ha una copia identica di questo file in
 * com.peppedess.wattmeter.battery.WatchProtocol: se cambi una chiave qui,
 * cambiala anche li, altrimenti smettono di capirsi.
 */
object WatchProtocol {
    const val PATH = "/wattmeter/battery"
    const val KEY_LEVEL = "level"
    const val KEY_POWER = "power"
    const val KEY_STATUS = "status"
    const val KEY_ETA = "eta"
    const val KEY_CHARGING = "charging"
    const val KEY_TIMESTAMP = "timestamp"
}
