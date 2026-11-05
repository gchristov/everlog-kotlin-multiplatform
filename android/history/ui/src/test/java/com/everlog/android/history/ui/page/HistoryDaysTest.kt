package com.everlog.android.history.ui.page

import com.everlog.android.common.data.model.workout.ELWorkout
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.ZoneId

class HistoryDaysTest {

    private val zone = ZoneId.of("Europe/London")
    private val today = LocalDate.of(2026, 11, 5)

    @Test
    fun `days run oldest first, each with its workouts earliest first`() {
        val evening = finished(today, hour = 19)
        val morning = finished(today, hour = 7)
        val lastWeek = finished(today.minusDays(7), hour = 12)

        val days = historyDays(listOf(evening, lastWeek, morning), zone)

        assertThat(days).containsExactly(
            HistoryDay(today.minusDays(7), listOf(lastWeek)),
            HistoryDay(today, listOf(morning, evening)),
        ).inOrder()
    }

    @Test
    fun `a workout belongs to the day it finished in the phone's time zone`() {
        val lateNight = finished(today, hour = 23, minute = 30)

        assertThat(historyDays(listOf(lateNight), ZoneId.of("Europe/Sofia")).single().date).isEqualTo(today.plusDays(1))
    }

    @Test
    fun `the calendar runs from the oldest day shown to next month`() {
        val days = historyDays(listOf(finished(LocalDate.of(2026, 3, 14), hour = 9)), zone)

        val months = calendarMonths(days, today)

        assertThat(months.start).isEqualTo(YearMonth.of(2026, 3))
        assertThat(months.endInclusive).isEqualTo(YearMonth.of(2026, 12))
    }

    @Test
    fun `with nothing to show the calendar has this month and the next`() {
        val months = calendarMonths(emptyList(), today)

        assertThat(months.start).isEqualTo(YearMonth.of(2026, 11))
        assertThat(months.endInclusive).isEqualTo(YearMonth.of(2026, 12))
    }

    private fun finished(date: LocalDate, hour: Int, minute: Int = 0): ELWorkout {
        val completed = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        return ELWorkout(completedDate = completed).apply { createdDate = completed - 3_600_000 }
    }
}
