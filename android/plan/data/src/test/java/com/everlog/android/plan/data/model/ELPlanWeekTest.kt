package com.everlog.android.plan.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ELPlanWeekTest {

    @Test
    fun `days past the free ones are locked for users without Pro`() {
        val week = ELPlanWeek.newWeek()

        week.lockDays(freeDays = 3, userIsPro = false)

        assertThat(week.getDays().map { it.proLocked }).containsExactly(false, false, false, true, true, true, true).inOrder()
    }

    @Test
    fun `nothing is locked for Pro users`() {
        val week = ELPlanWeek.newWeek()

        week.lockDays(freeDays = 3, userIsPro = true)

        assertThat(week.getDays().none { it.proLocked }).isTrue()
    }

    @Test
    fun `a week with no more days than the free ones has none locked`() {
        val week = ELPlanWeek.newWeek()

        week.lockDays(freeDays = 7, userIsPro = false)

        assertThat(week.getDays().none { it.proLocked }).isTrue()
    }

    @Test
    fun `locking again after the user goes Pro unlocks the days`() {
        val week = ELPlanWeek.newWeek()
        week.lockDays(freeDays = 3, userIsPro = false)

        week.lockDays(freeDays = 3, userIsPro = true)

        assertThat(week.getDays().none { it.proLocked }).isTrue()
    }
}
