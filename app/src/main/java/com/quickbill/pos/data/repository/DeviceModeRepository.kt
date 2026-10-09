/*
 * QuickBill + QuickKitchen
 *
 * Author: Dhivakar
 * Role: Android Developer
 *
 * Copyright (c) 2026 Dhivakar
 *
 * This file is part of the QuickBill + QuickKitchen project.
 * The original implementation and modifications in this file were
 * created by Dhivakar for the project/assignment.
 *
 * QuickBill-QuickKitchen-Author: Dhivakar
 *
 * Do not remove or alter this attribution notice.
 */

package com.quickbill.pos.data.repository

import android.content.Context
import com.quickbill.pos.data.model.kds.DeviceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceModeRepository(context: Context) {
    private val prefs = context.getSharedPreferences("quickbill_device_mode_prefs", Context.MODE_PRIVATE)

    private val _deviceMode = MutableStateFlow<DeviceMode?>(loadSavedDeviceMode())
    val deviceMode: StateFlow<DeviceMode?> = _deviceMode.asStateFlow()

    private fun loadSavedDeviceMode(): DeviceMode? {
        val saved = prefs.getString("selected_device_mode", null) ?: return null
        return try {
            DeviceMode.valueOf(saved)
        } catch (_: Exception) {
            null
        }
    }

    fun setDeviceMode(mode: DeviceMode?) {
        _deviceMode.value = mode
        if (mode != null) {
            prefs.edit().putString("selected_device_mode", mode.name).apply()
        } else {
            prefs.edit().remove("selected_device_mode").apply()
        }
    }
}
