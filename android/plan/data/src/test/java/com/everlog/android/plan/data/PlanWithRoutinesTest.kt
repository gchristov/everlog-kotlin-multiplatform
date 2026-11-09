package com.everlog.android.plan.data

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.plan.data.model.ELPlan
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlanWithRoutinesTest {

    private val legs = ELRoutine(uuid = "legs", name = "Legs")
    private val push = ELRoutine(uuid = "push", name = "Push")
    private val pull = ELRoutine(uuid = "pull", name = "Pull")

    // One week: legs on the first day, push on the second, rest after
    private fun plan(): ELPlan {
        val plan = ELPlan.newPlan("user")
        plan.weeks.single().getDays().forEachIndexed { index, day ->
            when (index) {
                0 -> day.setRoutine(legs)
                1 -> day.setRoutine(push)
                else -> day.setRest(true)
            }
        }
        return plan
    }

    @Test
    fun `each plan gets the routines its days use, and only those`() {
        val joined = withRoutines(listOf(plan()), listOf(legs, push, pull)).single()

        assertThat(joined.routines.keys).containsExactly("legs", "push")
        val days = joined.plan.weeks.single().getDays()
        assertThat(joined.routineFor(days[0])).isEqualTo(legs)
        assertThat(joined.routineFor(days[1])).isEqualTo(push)
        assertThat(joined.routineFor(days[2])).isNull()
    }

    @Test
    fun `a day whose routine was deleted has none`() {
        val joined = withRoutines(listOf(plan()), listOf(legs)).single()

        assertThat(joined.routineFor(joined.plan.weeks.single().getDays()[1])).isNull()
    }

    @Test
    fun `the plan is left as read`() {
        val plan = plan()
        val days = plan.weeks.single().getDays()
        days.forEach { it.setRoutine(null) }
        days[0].routineUuid = "legs"

        withRoutines(listOf(plan), listOf(legs))

        assertThat(days[0].getRoutine()).isNull()
    }
}
