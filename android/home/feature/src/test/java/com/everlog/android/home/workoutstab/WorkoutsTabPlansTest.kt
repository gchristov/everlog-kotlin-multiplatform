package com.everlog.android.home.workoutstab

import com.everlog.android.plan.data.PlanWithRoutines
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutsTabPlansTest {

    private val push = template("push", "Push day")
    private val legs = template("legs", "Legs")

    @Test
    fun `the plan being followed shows on its own, with its week`() {
        val ongoing = plan("strength", push, legs, done = 1)
        val other = plan("cut", push, legs).apply { name = "Summer cut" }

        val (active, others) = workoutsTabPlans(listOf(PlanWithRoutines(ongoing, emptyMap()), PlanWithRoutines(other, emptyMap())), ongoing)

        assertThat(active).isEqualTo(
            WorkoutsTabActivePlan(
                plan = WorkoutsTabPlan("strength", "Strength block", imageUrl = null, weeks = 1, daysPerWeek = 2),
                weekNumber = 1,
                doneThisWeek = 1,
                workoutsThisWeek = 2,
                next = null,
                progress = 14,
            ),
        )
        assertThat(others.map { it.name }).containsExactly("Summer cut")
    }

    @Test
    fun `the next workout day is named, a rest day isn't`() {
        val (active, _) = workoutsTabPlans(emptyList(), plan("strength", push, legs, done = 2))

        assertThat(active!!.next).isEqualTo("Legs")
    }

    @Test
    fun `without a plan being followed every plan is listed`() {
        val (active, others) = workoutsTabPlans(listOf(PlanWithRoutines(plan("strength", push, legs), emptyMap())), ongoing = null)

        assertThat(active).isNull()
        assertThat(others.single().daysPerWeek).isEqualTo(2)
    }

    @Test
    fun `templates in the plan being followed say so`() {
        val templates = workoutsTabTemplates(listOf(push, legs, template("arms")), plan("strength", push, legs))

        assertThat(templates.map { it.inPlan }).containsExactly(true, true, false).inOrder()
    }
}
