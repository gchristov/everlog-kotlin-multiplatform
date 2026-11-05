package com.everlog.android.statistics.data

import com.everlog.android.common.test.InMemorySharedPreferences
import com.everlog.android.common.test.install
import com.everlog.android.common.test.uninstall
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class RealStatisticsPreferencesTest {

    private val preferences = RealStatisticsPreferences()
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
    fun `opens on this week until a period is picked, then on that one`() {
        assertThat(preferences.period()).isEqualTo(StatsPeriod.Week)

        preferences.setPeriod(StatsPeriod.Last12Months)

        assertThat(RealStatisticsPreferences().period()).isEqualTo(StatsPeriod.Last12Months)
    }

    @Test
    fun `a period it doesn't know opens this week`() {
        stored.edit().putString("STATISTICS_PERIOD", "Overall").apply()

        assertThat(preferences.period()).isEqualTo(StatsPeriod.Week)
    }
}
