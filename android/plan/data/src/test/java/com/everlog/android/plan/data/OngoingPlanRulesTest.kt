package com.everlog.android.plan.data

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.plan.data.model.ELPlan
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OngoingPlanRulesTest {

    private val legs = ELRoutine(uuid = "legs", name = "Legs")

    // A plan of [weeks] weeks with legs on each week's first day and rest on the others, and [done] days done
    private fun plan(weeks: Int = 2, done: Int = 0): ELPlan {
        val plan = ELPlan.newPlan("user")
        repeat(weeks - 1) { plan.addNewWeek() }
        plan.weeks.forEach { week ->
            week.getDays().forEachIndexed { index, day -> if (index == 0) day.setRoutine(legs) else day.setRest(true) }
        }
        plan.weeks.flatMap { it.getDays() }.take(done).forEach { it.complete = true }
        return plan
    }

    @Test
    fun `the current week holds the next day to do`() {
        val week = plan(weeks = 2, done = 9).currentWeek()!!

        assertThat(week.index).isEqualTo(1)
        assertThat(week.weekCount).isEqualTo(2)
        assertThat(week.days).hasSize(7)
        assertThat(week.nextDay).isEqualTo(2)
    }

    @Test
    fun `a new plan starts on its first day`() {
        val week = plan().currentWeek()!!

        assertThat(week.index).isEqualTo(0)
        assertThat(week.nextDay).isEqualTo(0)
    }

    @Test
    fun `a done plan stays on its last week with no next day`() {
        val week = plan(weeks = 2, done = 14).currentWeek()!!

        assertThat(week.index).isEqualTo(1)
        assertThat(week.nextDay).isNull()
    }

    @Test
    fun `a plan without weeks has no current week`() {
        assertThat(ELPlan().currentWeek()).isNull()
    }

    @Test
    fun `the next step is the day's workout, a rest day or finishing`() {
        assertThat(plan(done = 0).nextStep()).isEqualTo(PlanNextStep.Workout(legs))
        assertThat(plan(done = 1).nextStep()).isEqualTo(PlanNextStep.RestDay)
        assertThat(plan(weeks = 1, done = 7).nextStep()).isEqualTo(PlanNextStep.Finish)
    }

    @Test
    fun `a day with nothing set is started as a rest day`() {
        val plan = plan()
        plan.getNextDay()!!.setRoutine(null)

        assertThat(plan.nextStep()).isEqualTo(PlanNextStep.RestDay)
    }

    @Test
    fun `skipping turns the next day into a done rest day, leaving the plan as it was`() {
        val plan = plan()

        val skipped = plan.withNextDaySkipped()

        val day = skipped.weeks[0].getDays()[0]
        assertThat(day.getRest()).isTrue()
        assertThat(day.getRoutine()).isNull()
        assertThat(day.complete).isTrue()
        assertThat(skipped.currentWeek()!!.nextDay).isEqualTo(1)
        assertThat(plan.getNextDay()!!.complete).isFalse()
    }

    @Test
    fun `completing a rest day moves on to the next one`() {
        val done = plan(done = 1).withNextRestDayDone()

        assertThat(done.weeks[0].getDays()[1].complete).isTrue()
        assertThat(done.currentWeek()!!.nextDay).isEqualTo(2)
    }

    @Test
    fun `skipping or completing a done plan changes nothing`() {
        val plan = plan(weeks = 1, done = 7)

        assertThat(plan.withNextDaySkipped()).isSameInstanceAs(plan)
        assertThat(plan.withNextRestDayDone()).isSameInstanceAs(plan)
    }
}
