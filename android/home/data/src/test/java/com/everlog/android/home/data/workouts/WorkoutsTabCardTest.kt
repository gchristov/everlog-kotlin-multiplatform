package com.everlog.android.home.data.workouts

import com.everlog.android.plan.data.model.ELPlan
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutsTabCardTest {

    private val push = template("push", "Push day", createdDate = 2)
    private val pull = template("pull", "Pull day", createdDate = 1)
    private val legs = template("legs", "Legs", createdDate = 3)
    private val templates = listOf(push, pull, legs)
    private val rotation = history("push", "pull", "legs", "push", "pull", "legs", "push", "pull", "legs", "push")

    // One week: push, a rest day, legs, then rest; [done] days already done
    private fun plan(done: Int): ELPlan {
        val plan = ELPlan.newPlan("user")
        plan.weeks.single().getDays().forEachIndexed { index, day ->
            when (index) {
                0 -> day.setRoutine(push)
                2 -> day.setRoutine(legs)
                else -> day.setRest(true)
            }
            day.complete = index < done
        }
        return plan
    }

    @Test
    fun `the ongoing plan's next workout comes first`() {
        val card = workoutsTabCard(templates, rotation, plan(done = 2))

        assertThat(card).isInstanceOf(WorkoutsTabCard.PlanNext::class.java)
        card as WorkoutsTabCard.PlanNext
        assertThat(card.routine.uuid).isEqualTo("legs")
        assertThat(card.weekNumber).isEqualTo(1)
        assertThat(card.dayNumber).isEqualTo(3)
    }

    @Test
    fun `a plan whose next day is a rest day leaves the card to the suggestion`() {
        assertThat(workoutsTabCard(templates, rotation, plan(done = 1))).isInstanceOf(WorkoutsTabCard.Suggested::class.java)
    }

    @Test
    fun `a finished plan leaves the card to the suggestion`() {
        assertThat(workoutsTabCard(templates, rotation, plan(done = 7))).isInstanceOf(WorkoutsTabCard.Suggested::class.java)
    }

    @Test
    fun `a clear pattern suggests the next template, saying what it follows`() {
        assertThat(workoutsTabCard(templates, rotation, ongoingPlan = null))
            .isEqualTo(WorkoutsTabCard.Suggested(template = pull, afterName = "Push day", times = 3, of = 3))
    }

    @Test
    fun `the latest template's name comes from its workouts once it's deleted`() {
        val card = workoutsTabCard(listOf(pull, legs), rotation, ongoingPlan = null) as WorkoutsTabCard.Suggested

        assertThat(card.afterName).isEqualTo("push")
    }

    @Test
    fun `no pattern, no card`() {
        assertThat(workoutsTabCard(templates, history("push", "legs", "pull", "push"), ongoingPlan = null)).isNull()
    }

    @Test
    fun `templates but nothing done lately start with the one saved first`() {
        assertThat(workoutsTabCard(templates, history(), ongoingPlan = null)).isEqualTo(WorkoutsTabCard.StartHere(pull))
    }

    @Test
    fun `no templates means getting started, history or not`() {
        assertThat(workoutsTabCard(emptyList(), history = null, ongoingPlan = null)).isEqualTo(WorkoutsTabCard.GetStarted)
    }

    @Test
    fun `without the history yet only the plan can show`() {
        assertThat(workoutsTabCard(templates, history = null, ongoingPlan = null)).isNull()
        assertThat(workoutsTabCard(templates, history = null, plan(done = 0))).isInstanceOf(WorkoutsTabCard.PlanNext::class.java)
    }
}
