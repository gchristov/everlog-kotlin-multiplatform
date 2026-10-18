package com.everlog.data.controllers.workoutsession

import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutSessionRulesTest {

    private val ids = SessionIds()
    private val now = 1_000L

    // A single exercise with 2 sets, then a superset with 2 rounds
    private val single = block(exercises = 1, rounds = 2)
    private val superset = block(exercises = 2, rounds = 2)
    private val blocks = listOf(single, superset)

    // Completing

    @Test
    fun `completing a single exercise's set moves on to the next and starts rest`() {
        val completion = blocks.completeCurrent(ref(single, 0), now)

        assertThat(completion.blocks.set(ref(single, 0))?.completedDate).isEqualTo(now)
        assertThat(completion.current).isEqualTo(ref(single, 1))
        assertThat(completion.startsRest).isTrue()
    }

    @Test
    fun `completing a superset's first exercise goes on to the second without rest`() {
        val completion = blocks.completeCurrent(ref(superset, 0, index = 0), now)

        assertThat(completion.current).isEqualTo(ref(superset, 0, index = 1))
        assertThat(completion.startsRest).isFalse()
    }

    @Test
    fun `a superset's round goes back to the earliest exercise not done`() {
        val secondDone = blocks.completeSet(ref(superset, 0, index = 1), current = null, now).blocks

        val completion = secondDone.completeCurrent(ref(superset, 0, index = 0), now)

        // The round is done, so it moves on to the next round with rest
        assertThat(completion.current).isEqualTo(ref(superset, 1, index = 0))
        assertThat(completion.startsRest).isTrue()
    }

    @Test
    fun `completing the last set not done ends without rest`() {
        val allButLast = blocks.refs().dropLast(1).fold(blocks) { acc, ref -> acc.completeSet(ref, current = null, now).blocks }

        val completion = allButLast.completeCurrent(blocks.refs().last(), now)

        assertThat(completion.current).isNull()
        assertThat(completion.startsRest).isFalse()
    }

    @Test
    fun `the next set not done goes round to skipped ones`() {
        val firstSkipped = blocks.refs().drop(1).fold(blocks) { acc, ref -> acc.completeSet(ref, current = null, now).blocks }

        assertThat(firstSkipped.nextOpenAfter(blocks.refs().last())).isEqualTo(ref(single, 0))
    }

    @Test
    fun `completing another set keeps the current one`() {
        val completion = blocks.completeSet(ref(single, 1), current = ref(single, 0), now)

        assertThat(completion.current).isEqualTo(ref(single, 0))
        assertThat(completion.startsRest).isTrue()
    }

    @Test
    fun `completing one exercise of a superset's round elsewhere starts no rest`() {
        val completion = blocks.completeSet(ref(superset, 1, index = 0), current = ref(single, 0), now)

        assertThat(completion.startsRest).isFalse()
    }

    @Test
    fun `completing a round completes each exercise's set and moves the current set on`() {
        val completion = blocks.completeRound(superset.id, superset.rounds[0].id, current = ref(superset, 0, index = 0), now)

        assertThat(completion.blocks.round(superset.id, superset.rounds[0].id)?.done).isTrue()
        assertThat(completion.current).isEqualTo(ref(superset, 1, index = 0))
    }

    @Test
    fun `undoing a set keeps its values and start`() {
        val done = blocks.completeSet(ref(single, 0), current = null, now).blocks

        val set = done.uncompleteSet(ref(single, 0)).set(ref(single, 0))!!

        assertThat(set.done).isFalse()
        assertThat(set.startedDate).isEqualTo(now)
        assertThat(set.reps).isEqualTo(8)
    }

    // Values

    @Test
    fun `nothing under 1 clears a value`() {
        val cleared = blocks.setValue(ref(single, 0), ValueField.Reps, 0f).setValue(ref(single, 0), ValueField.Weight, 0f)

        assertThat(cleared.set(ref(single, 0))?.reps).isNull()
        assertThat(cleared.set(ref(single, 0))?.weightKg).isNull()
    }

    @Test
    fun `switching mode keeps the other value`() {
        val switched = blocks.setMode(ref(single, 0), SetMode.Time).setValue(ref(single, 0), ValueField.Time, 30f).setMode(ref(single, 0), SetMode.Reps)

        assertThat(switched.set(ref(single, 0))?.reps).isEqualTo(8)
        assertThat(switched.set(ref(single, 0))?.timeSeconds).isEqualTo(30)
    }

    // Adding and deleting

    @Test
    fun `an added round copies the last round's values, not done`() {
        val done = blocks.completeRound(superset.id, superset.rounds[1].id, current = null, now).blocks

        val added = done.addRound(superset.id, ids).block(superset.id)!!.rounds.last()

        assertThat(added.sets.map { it.reps }).containsExactly(8, 8)
        assertThat(added.sets.none { it.done || it.startedDate != null }).isTrue()
    }

    @Test
    fun `deleting one exercise's set in a superset keeps the round`() {
        val (deleted, undo) = blocks.deleteSet(ref(superset, 0, index = 1))!!

        assertThat(deleted.set(ref(superset, 0, index = 1))?.removed).isTrue()
        assertThat(deleted.refs()).doesNotContain(ref(superset, 0, index = 1))
        assertThat(deleted.restore(undo)).isEqualTo(blocks)
    }

    @Test
    fun `deleting the last set left in a round deletes the round`() {
        val (oneLeft, _) = blocks.deleteSet(ref(superset, 0, index = 1))!!

        val (deleted, undo) = oneLeft.deleteSet(ref(superset, 0, index = 0))!!

        assertThat(deleted.block(superset.id)?.rounds).hasSize(1)
        assertThat(deleted.restore(undo)).isEqualTo(oneLeft)
    }

    @Test
    fun `a deleted round goes back where it was`() {
        val (deleted, undo) = blocks.deleteRound(single.id, single.rounds[0].id)!!

        assertThat(deleted.restore(undo)).isEqualTo(blocks)
    }

    @Test
    fun `the current set moves on from a block about to be deleted`() {
        val next = blocks.nextOpenOutside(ref(single, 0), blockId = single.id)

        assertThat(next).isEqualTo(ref(superset, 0, index = 0))
    }

    @Test
    fun `blocks can be moved`() {
        assertThat(blocks.moveBlock(1, 0)).containsExactly(superset, single).inOrder()
        assertThat(blocks.moveBlock(0, 5)).isEqualTo(blocks)
    }

    @Test
    fun `picked exercises are added at the end`() {
        val bench = ELExercise(uuid = "bench")
        val group = ELExerciseGroup(uuid = "picked", type = "SINGLE", exercises = mutableListOf(ELRoutineExercise("bench", bench, mutableListOf(ELSet()))))

        val added = blocks.addGroups(listOf(group), ids)

        assertThat(added.last().uuid).isEqualTo("picked")
        assertThat(added.nextOpenAfter(blocks.refs().last())).isEqualTo(SetRef(added.last().id, added.last().rounds[0].id, 0))
    }

    @Test
    fun `changing the rest changes every block's`() {
        assertThat(blocks.withRestSeconds(90).map { it.restSeconds }).containsExactly(90, 90)
    }

    private fun block(exercises: Int, rounds: Int) = WorkoutBlock(
        id = ids.next(),
        uuid = null,
        type = if (exercises == 1) "SINGLE" else "SUPER",
        restSeconds = 60,
        exercises = List(exercises) { WorkoutExercise("exercise-$it", null) },
        rounds = List(rounds) { WorkoutRound(ids.next(), List(exercises) { WorkoutSet(reps = 8, weightKg = 20f) }) },
    )

    private fun ref(block: WorkoutBlock, round: Int, index: Int = 0) = SetRef(block.id, block.rounds[round].id, index)
}
