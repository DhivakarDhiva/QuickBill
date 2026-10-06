package com.quickbill.pos.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class KdsSettings(
    val kitchenName: String = "Main Kitchen",
    val warningThresholdMinutes: Int = 5,
    val soundAlertEnabled: Boolean = true,
    val vibrateAlertEnabled: Boolean = true,
    val serverPort: Int = 8080,
    val manualKdsIp: String = "",
    val manualKdsPort: Int = 8080
)

class KdsSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("quickbill_kds_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<KdsSettings> = _settings.asStateFlow()

    private fun loadSettings(): KdsSettings {
        return KdsSettings(
            kitchenName = prefs.getString("kitchen_name", "Main Kitchen") ?: "Main Kitchen",
            warningThresholdMinutes = prefs.getInt("warning_threshold_minutes", 5),
            soundAlertEnabled = prefs.getBoolean("sound_alert_enabled", true),
            vibrateAlertEnabled = prefs.getBoolean("vibrate_alert_enabled", true),
            serverPort = prefs.getInt("server_port", 8080),
            manualKdsIp = prefs.getString("manual_kds_ip", "") ?: "",
            manualKdsPort = prefs.getInt("manual_kds_port", 8080)
        )
    }

    fun updateSettings(settings: KdsSettings) {
        _settings.value = settings
        prefs.edit()
            .putString("kitchen_name", settings.kitchenName)
            .putInt("warning_threshold_minutes", settings.warningThresholdMinutes)
            .putBoolean("sound_alert_enabled", settings.soundAlertEnabled)
            .putBoolean("vibrate_alert_enabled", settings.vibrateAlertEnabled)
            .putInt("server_port", settings.serverPort)
            .putString("manual_kds_ip", settings.manualKdsIp)
            .putInt("manual_kds_port", settings.manualKdsPort)
            .apply()
    }

    fun setManualKdsAddress(ip: String, port: Int) {
        val updated = _settings.value.copy(manualKdsIp = ip, manualKdsPort = port)
        updateSettings(updated)
    }
}
