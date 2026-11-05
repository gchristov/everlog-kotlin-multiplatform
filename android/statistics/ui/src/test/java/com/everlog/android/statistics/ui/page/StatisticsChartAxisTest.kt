package com.everlog.android.statistics.ui.page

import com.everlog.android.statistics.data.StatsPeriod
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalDate

class StatisticsChartAxisTest {

    private val today = LocalDate.of(2026, 11, 4)
    private lateinit var locale: Locale

    @Before
    fun setUp() {
        locale = Locale.getDefault()
        Locale.setDefault(Locale.UK)
    }

    @After
    fun tearDown() {
        Locale.setDefault(locale)
    }

    @Test
    fun `days keep their order and label as day and month`() {
        val x = StatsPeriod.Week.chartX(LocalDate.of(2026, 11, 2))

        assertThat(StatsPeriod.Week.chartX(LocalDate.of(2026, 11, 3))).isEqualTo(x + 1)
        assertThat(StatsPeriod.Week.chartLabel(x, today)).isEqualTo("2\nNov")
    }

    @Test
    fun `months keep their order across the new year`() {
        val december = StatsPeriod.Last12Months.chartX(LocalDate.of(2025, 12, 1))
        val january = StatsPeriod.Last12Months.chartX(LocalDate.of(2026, 1, 1))

        assertThat(january).isEqualTo(december + 1)
        assertThat(StatsPeriod.Last12Months.chartLabel(december, today)).isEqualTo("Dec, 2025")
        assertThat(StatsPeriod.Last12Months.chartLabel(january, today)).isEqualTo("Jan")
    }

    @Test
    fun `a day from another year says which`() {
        val x = StatsPeriod.Month.chartX(LocalDate.of(2025, 12, 31))

        assertThat(StatsPeriod.Month.chartLabel(x, today)).isEqualTo("31\nDec, 2025")
    }
}
