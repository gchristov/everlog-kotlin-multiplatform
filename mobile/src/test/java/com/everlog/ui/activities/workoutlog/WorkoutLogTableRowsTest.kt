package com.everlog.ui.activities.workoutlog

import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutExercise
import com.everlog.data.controllers.workoutsession.WorkoutRound
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.model.exercise.ELExercise
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutLogTableRowsTest {

    private val single = block(1, "bench", rounds = listOf(listOf(WorkoutSet(reps = 8, weightKg = 60f)), listOf(WorkoutSet(reps = 8))))
    private val superset = block(
        2, "fly", "raise",
        rounds = listOf(
            listOf(WorkoutSet(reps = 12), WorkoutSet(reps = 12)),
            listOf(WorkoutSet(removed = true), WorkoutSet(reps = 10)),
        ),
    )
    private val view = TableView(unit = WeightUnit.KILOGRAM, unitLabel = "KG")

    private fun block(id: Long, vararg names: String, rounds: List<List<WorkoutSet>>) = WorkoutBlock(
        id = id,
        uuid = null,
        type = null,
        restSeconds = 60,
        exercises = names.map { WorkoutExercise(it, ELExercise(uuid = it, name = it)) },
        rounds = rounds.mapIndexed { r, sets -> WorkoutRound(id * 10 + r, sets) },
    )

    private fun ref(block: WorkoutBlock, round: Int, exercise: Int = 0) = SetRef(block.id, block.rounds[round].id, exercise)

    @Test
    fun `a single exercise's rows are numbered by set, a superset's by set and letter, skipping removed sets`() {
        assertThat(single.rows(view).map { it.number }).containsExactly("1", "2").inOrder()
        assertThat(superset.rows(view).map { it.number }).containsExactly("1a", "1b", "2b").inOrder()
    }

    @Test
    fun `the first row left in a round starts it, for its divider`() {
        assertThat(superset.rows(view).map { it.startsRound }).containsExactly(true, false, true).inOrder()
    }

    @Test
    fun `values show as they're stored, in the user's unit, with a dash for none`() {
        val rows = single.rows(view.copy(unit = WeightUnit.POUND, unitLabel = "LB"))

        assertThat(rows[0].countText).isEqualTo("8")
        assertThat(rows[0].weightText).isEqualTo("132.28")
        assertThat(rows[1].weightText).isEqualTo(EmptyValue)
    }

    @Test
    fun `the focused cell shows what's typed, and a typed time reads as a clock`() {
        val typedWeight = single.rows(view.copy(focus = CellFocus(ref(single, 0), ValueField.Weight), typed = "62.")).first()
        assertThat(typedWeight.weightText).isEqualTo("62.")
        assertThat(typedWeight.weightFocused).isTrue()
        assertThat(typedWeight.armed).isTrue()

        val timed = single.copy(rounds = single.rounds.map { round -> round.copy(sets = round.sets.map { it.copy(mode = SetMode.Time) }) })
        val typedTime = timed.rows(view.copy(focus = CellFocus(ref(timed, 0), ValueField.Time), typed = "130")).first()
        assertThat(typedTime.countText).isEqualTo("1:30")
        assertThat(typedTime.countFocused).isTrue()
        assertThat(typedTime.countField).isEqualTo(ValueField.Time)
    }

    @Test
    fun `a running set timer counts down in its time cell, which lights its tick`() {
        val row = single.rows(view.copy(timing = Countdown(ref(single, 1), seconds = 65))).last()

        assertThat(row.countText).isEqualTo("1:05")
        assertThat(row.timing).isTrue()
        assertThat(row.armed).isTrue()
    }

    @Test
    fun `last time shows the last session's set at the same place`() {
        val lastTime = mapOf("bench" to listOf(WorkoutSet(reps = 10, weightKg = 55f)))

        val rows = single.rows(view.copy(lastTime = lastTime))

        assertThat(rows.map { it.lastTime }).containsExactly("10 × 55 kg", null).inOrder()
    }

    @Test
    fun `a weight filled in from history is marked as a suggestion`() {
        val suggested = single.copy(rounds = listOf(WorkoutRound(10, listOf(WorkoutSet(weightKg = 60f, suggestedKg = 60f)))))

        assertThat(suggested.rows(view).single().suggested).isTrue()
        assertThat(single.rows(view).first().suggested).isFalse()
    }

    @Test
    fun `the selected row and a timed set's start button follow the view`() {
        val rows = single.rows(view.copy(selected = ref(single, 1), startsTimer = { it == ref(single, 0) }))

        assertThat(rows.map { it.selected }).containsExactly(false, true).inOrder()
        assertThat(rows.map { it.startsTimer }).containsExactly(true, false).inOrder()
    }
}
