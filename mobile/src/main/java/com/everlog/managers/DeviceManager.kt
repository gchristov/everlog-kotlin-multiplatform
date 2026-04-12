package com.everlog.managers

import android.text.TextUtils
import com.everlog.managers.preferences.PreferencesManager
import java.util.*

class DeviceManager : PreferencesManager() {

    private enum class PreferenceKeys {
        DEVICE_ID,
    }

    companion object {

        @JvmField
        val manager = DeviceManager()
    }

    fun deviceId(): String? {
        var id = getPreference(PreferenceKeys.DEVICE_ID.name, "")
        if (TextUtils.isEmpty(id)) {
            id = UUID.randomUUID().toString()
            setDeviceId(id)
        }
        return id
    }

    private fun setDeviceId(value: String) {
        savePreference(value, PreferenceKeys.DEVICE_ID.name)
    }
}