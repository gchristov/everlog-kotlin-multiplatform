package com.everlog.android.history.data

import com.everlog.android.common.test.InMemorySharedPreferences
import com.everlog.android.common.test.install
import com.everlog.android.common.test.uninstall
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class RealHistoryPreferencesTest {

    private val preferences = RealHistoryPreferences()
    private lateinit var stored: InMemorySharedPreferences

    @Before
    fun setUp() {
        stored = InMemorySharedPreferences.install()
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    @Test
    fun `opens on the list until a view is picked, then on that one`() {
        assertThat(preferences.view()).isEqualTo(HistoryView.List)

        preferences.setView(HistoryView.Calendar)

        assertThat(RealHistoryPreferences().view()).isEqualTo(HistoryView.Calendar)
    }

    @Test
    fun `a view it doesn't know opens the list`() {
        stored.edit().putString("HISTORY_VIEW", "Timeline").apply()

        assertThat(preferences.view()).isEqualTo(HistoryView.List)
    }
}
