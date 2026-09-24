package com.everlog.testutil

import android.content.SharedPreferences
import com.everlog.managers.preferences.PreferencesManager

/**
 * A map-backed SharedPreferences, so code that reads settings (e.g. SettingsManager) can run
 * in plain JVM unit tests. Install it with [install] and remove it with [uninstall].
 */
class InMemorySharedPreferences : SharedPreferences {

    companion object {

        fun install(): InMemorySharedPreferences {
            val preferences = InMemorySharedPreferences()
            PreferencesManager.setPreferencesProvider { preferences }
            return preferences
        }

        fun uninstall() {
            PreferencesManager.setPreferencesProvider(null)
        }
    }

    private val values = HashMap<String, Any?>()

    override fun getAll(): Map<String, *> = HashMap(values)

    override fun getString(key: String, defValue: String?): String? = get(key, defValue)

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = get(key, defValues)

    override fun getInt(key: String, defValue: Int): Int = get(key, defValue)

    override fun getLong(key: String, defValue: Long): Long = get(key, defValue)

    override fun getFloat(key: String, defValue: Float): Float = get(key, defValue)

    override fun getBoolean(key: String, defValue: Boolean): Boolean = get(key, defValue)

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}

    @Suppress("UNCHECKED_CAST")
    private fun <T> get(key: String, defValue: T): T = if (values.containsKey(key)) values[key] as T else defValue

    private inner class Editor : SharedPreferences.Editor {

        private val changes = HashMap<String, Any?>()
        private val removals = HashSet<String>()
        private var clear = false

        override fun putString(key: String, value: String?) = apply { changes[key] = value }

        override fun putStringSet(key: String, values: Set<String>?) = apply { changes[key] = values }

        override fun putInt(key: String, value: Int) = apply { changes[key] = value }

        override fun putLong(key: String, value: Long) = apply { changes[key] = value }

        override fun putFloat(key: String, value: Float) = apply { changes[key] = value }

        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }

        override fun remove(key: String) = apply { removals.add(key) }

        override fun clear() = apply { clear = true }

        override fun commit(): Boolean {
            if (clear) values.clear()
            removals.forEach { values.remove(it) }
            values.putAll(changes)
            return true
        }

        override fun apply() {
            commit()
        }
    }
}
