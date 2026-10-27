package com.everlog.data.controllers.templatesession

import com.everlog.data.controllers.workoutsession.SessionIds
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutExercise
import com.everlog.data.controllers.workoutsession.WorkoutRound
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TemplateSessionRulesTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")
    private val fly = ELExercise(uuid = "fly", name = "Fly")
    private val raise = ELExercise(uuid = "raise", name = "Lateral raise")

    @Test
    fun `a picked exercise starts with three sets of ten`() {
        val picked = ELExerciseGroup(uuid = "g", type = "SINGLE", exercises = mutableListOf(ELRoutineExercise.buildRoutineExercise(bench)))

        val block = emptyList<WorkoutBlock>().addTemplateGroups(listOf(picked), SessionIds()).single()

        assertThat(block.rounds.map { it.sets }).containsExactly(
            listOf(WorkoutSet(reps = 10)),
            listOf(WorkoutSet(reps = 10)),
            listOf(WorkoutSet(reps = 10)),
        ).inOrder()
        assertThat(block.rounds.map { it.id }.toSet()).hasSize(3)
    }

    @Test
    fun `a picked superset gets three rounds of ten for each exercise, after the blocks already there`() {
        val existing = listOf(block(1, sets = listOf(listOf(WorkoutSet(reps = 5)))))
        val picked = ELExerciseGroup(
            uuid = "g",
            type = "SUPER",
            exercises = mutableListOf(ELRoutineExercise.buildRoutineExercise(fly), ELRoutineExercise.buildRoutineExercise(raise)),
        )

        val blocks = existing.addTemplateGroups(listOf(picked), SessionIds(start = 100))

        assertThat(blocks.first()).isEqualTo(existing.single())
        assertThat(blocks.last().rounds.map { round -> round.sets.map { it.reps } }).containsExactly(listOf(10, 10), listOf(10, 10), listOf(10, 10))
    }

    @Test
    fun `a picked group that already has targets keeps them`() {
        val picked = ELExerciseGroup(uuid = "g", type = "SINGLE", exercises = mutableListOf(
            ELRoutineExercise("item", bench, mutableListOf(ELSet(requiredTimeSeconds = 60))),
        ))

        val block = emptyList<WorkoutBlock>().addTemplateGroups(listOf(picked), SessionIds()).single()

        assertThat(block.rounds.map { it.sets }).containsExactly(listOf(WorkoutSet(mode = SetMode.Time, timeSeconds = 60)))
    }

    @Test
    fun `switching an exercise to time switches all its sets, starting those without a time at the default`() {
        val superset = block(1, sets = listOf(
            listOf(WorkoutSet(reps = 12), WorkoutSet(reps = 10)),
            listOf(WorkoutSet(reps = 12, timeSeconds = 45), WorkoutSet(reps = 8)),
            listOf(WorkoutSet(removed = true), WorkoutSet(reps = 8)),
        ))

        val switched = listOf(superset).setExerciseMode(blockId = 1, exercise = 0, mode = SetMode.Time).single()

        assertThat(switched.rounds.map { it.sets[0] }).containsExactly(
            WorkoutSet(mode = SetMode.Time, reps = 12, timeSeconds = DefaultTimeSeconds),
            WorkoutSet(mode = SetMode.Time, reps = 12, timeSeconds = 45),
            WorkoutSet(removed = true),
        ).inOrder()
        assertThat(switched.rounds.map { it.sets[1] }).isEqualTo(superset.rounds.map { it.sets[1] })
    }

    @Test
    fun `switching back to reps brings back the reps it had, or the default`() {
        val timed = listOf(block(1, sets = listOf(listOf(WorkoutSet(reps = 12)), listOf(WorkoutSet(mode = SetMode.Time, timeSeconds = 30)))))

        val back = timed
            .setExerciseMode(blockId = 1, exercise = 0, mode = SetMode.Time)
            .setExerciseMode(blockId = 1, exercise = 0, mode = SetMode.Reps)
            .single()

        assertThat(back.rounds.map { it.sets.single().mode }).containsExactly(SetMode.Reps, SetMode.Reps)
        assertThat(back.rounds.map { it.sets.single().reps }).containsExactly(12, DefaultReps).inOrder()
    }

    @Test
    fun `the same time goes to every set of the exercise, and other blocks are left alone`() {
        val blocks = listOf(
            block(1, sets = listOf(listOf(WorkoutSet(mode = SetMode.Time, timeSeconds = 30)), listOf(WorkoutSet(reps = 10)))),
            block(2, sets = listOf(listOf(WorkoutSet(mode = SetMode.Time, timeSeconds = 20)))),
        )

        val changed = blocks.withTimeForAllSets(blockId = 1, exercise = 0, seconds = 50)

        assertThat(changed[0].rounds.map { it.sets.single() }).containsExactly(
            WorkoutSet(mode = SetMode.Time, timeSeconds = 50),
            WorkoutSet(mode = SetMode.Time, reps = 10, timeSeconds = 50),
        ).inOrder()
        assertThat(changed[1]).isEqualTo(blocks[1])
    }

    private fun block(id: Long, sets: List<List<WorkoutSet>>) = WorkoutBlock(
        id = id,
        uuid = null,
        type = if (sets.first().size > 1) "SUPER" else "SINGLE",
        restSeconds = 60,
        exercises = sets.first().indices.map { WorkoutExercise("item$it", ELExercise(uuid = "e$it")) },
        rounds = sets.mapIndexed { r, round -> WorkoutRound(id * 10 + r, round) },
    )
}
