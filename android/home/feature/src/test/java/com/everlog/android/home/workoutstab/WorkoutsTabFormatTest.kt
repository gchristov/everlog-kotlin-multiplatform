package com.everlog.android.home.workoutstab

import com.everlog.android.common.data.model.set.ELSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset

class WorkoutsTabFormatTest {

    private fun reps(vararg values: Int) = values.map { ELSet().apply { updateRequiredReps(it) } }

    private fun times(vararg seconds: Int) = seconds.map { ELSet().apply { updateRequiredTimeSeconds(it) } }

    @Test
    fun `the same reps on every set`() {
        assertThat(setsTarget(reps(8, 8, 8))).isEqualTo(SetsTarget.Reps(sets = 3, min = 8, max = 8))
    }

    @Test
    fun `different reps make a range`() {
        assertThat(setsTarget(reps(12, 10, 8))).isEqualTo(SetsTarget.Reps(sets = 3, min = 8, max = 12))
    }

    @Test
    fun `reps done count as well as targets`() {
        val sets = listOf(ELSet().apply { updateReps(10) }, ELSet().apply { updateRequiredReps(12) })

        assertThat(setsTarget(sets)).isEqualTo(SetsTarget.Reps(sets = 2, min = 10, max = 12))
    }

    @Test
    fun `timed sets`() {
        assertThat(setsTarget(times(30, 45))).isEqualTo(SetsTarget.Time(sets = 2, minSeconds = 30, maxSeconds = 45))
    }

    @Test
    fun `sets that don't say just count`() {
        assertThat(setsTarget(listOf(ELSet(), ELSet()))).isEqualTo(SetsTarget.Sets(2))
        assertThat(setsTarget(reps(8) + ELSet())).isEqualTo(SetsTarget.Sets(2))
    }

    private val today = LocalDate.of(2026, 11, 10)

    private fun daysAgo(days: Long) = today.minusDays(days).atTime(18, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `when a template was last done`() {
        fun ago(days: Long) = doneAgo(daysAgo(days), today, ZoneOffset.UTC)

        assertThat(ago(0)).isEqualTo(DoneAgo.Today)
        assertThat(ago(1)).isEqualTo(DoneAgo.Yesterday)
        assertThat(ago(6)).isEqualTo(DoneAgo.Days(6))
        assertThat(ago(13)).isEqualTo(DoneAgo.Days(13))
        assertThat(ago(14)).isEqualTo(DoneAgo.Weeks(2))
        assertThat(ago(56)).isEqualTo(DoneAgo.Weeks(8))
    }

    @Test
    fun `not in the history is not done recently`() {
        assertThat(doneAgo(null, today)).isEqualTo(DoneAgo.NotRecently)
    }
}
