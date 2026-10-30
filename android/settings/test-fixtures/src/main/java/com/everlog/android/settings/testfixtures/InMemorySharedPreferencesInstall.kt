package com.everlog.android.settings.testfixtures

import com.everlog.android.common.persistence.PreferencesManager
import com.everlog.android.common.test.InMemorySharedPreferences

/** Makes PreferencesManager, and so SettingsManager, read a fresh [InMemorySharedPreferences]. Undo with [uninstall]. */
fun InMemorySharedPreferences.Companion.install(): InMemorySharedPreferences {
    val preferences = InMemorySharedPreferences()
    PreferencesManager.setPreferencesProvider { preferences }
    return preferences
}

fun InMemorySharedPreferences.Companion.uninstall() {
    PreferencesManager.setPreferencesProvider(null)
}
