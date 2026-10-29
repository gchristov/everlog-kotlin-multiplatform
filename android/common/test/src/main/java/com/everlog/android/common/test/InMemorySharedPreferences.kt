package com.everlog.android.common.test

import android.content.SharedPreferences

/** A map-backed SharedPreferences, for code that reads preferences in plain JVM unit tests. */
class InMemorySharedPreferences : SharedPreferences {

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

        override fun putString(key: String, value: String?) = also { changes[key] = value }

        override fun putStringSet(key: String, values: Set<String>?) = also { changes[key] = values }

        override fun putInt(key: String, value: Int) = also { changes[key] = value }

        override fun putLong(key: String, value: Long) = also { changes[key] = value }

        override fun putFloat(key: String, value: Float) = also { changes[key] = value }

        override fun putBoolean(key: String, value: Boolean) = also { changes[key] = value }

        override fun remove(key: String) = also { removals.add(key) }

        override fun clear() = also { clear = true }

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

    // For helpers that install it in place of a module's preferences
    companion object
}
