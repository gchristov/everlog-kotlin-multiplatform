package com.everlog.android.common.test

import com.everlog.android.common.persistence.PreferencesManager

/** Makes PreferencesManager, and so SettingsManager, read a fresh [InMemorySharedPreferences]. Undo with [uninstall]. */
fun InMemorySharedPreferences.Companion.install(): InMemorySharedPreferences {
    val preferences = InMemorySharedPreferences()
    PreferencesManager.setPreferencesProvider { preferences }
    return preferences
}

fun InMemorySharedPreferences.Companion.uninstall() {
    PreferencesManager.setPreferencesProvider(null)
}
