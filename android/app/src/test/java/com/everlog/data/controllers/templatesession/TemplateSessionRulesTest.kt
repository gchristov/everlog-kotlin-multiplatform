package com.everlog.data.controllers.templatesession

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.session.ExerciseRef
import com.everlog.android.common.session.SessionIds
import com.everlog.android.common.session.SetMode
import com.everlog.android.common.session.WorkoutBlock
import com.everlog.android.common.session.WorkoutExercise
import com.everlog.android.common.session.WorkoutRound
import com.everlog.android.common.session.WorkoutSet
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

    // Grouping, ungrouping and deleting picked exercises

    private val benchItem = WorkoutExercise("bench-item", ELExercise(uuid = "bench"))
    private val flyItem = WorkoutExercise("fly-item", ELExercise(uuid = "fly"))
    private val raiseItem = WorkoutExercise("raise-item", ELExercise(uuid = "raise"))
    private val squatItem = WorkoutExercise("squat-item", ELExercise(uuid = "squat"))

    private fun single(id: Long, exercise: WorkoutExercise, vararg reps: Int) = WorkoutBlock(
        id = id,
        uuid = "u$id",
        type = "SINGLE",
        restSeconds = 60,
        exercises = listOf(exercise),
        rounds = reps.mapIndexed { r, n -> WorkoutRound(id * 10 + r, listOf(WorkoutSet(reps = n))) },
    )

    private fun superset(id: Long, a: WorkoutExercise, b: WorkoutExercise, rounds: Int) = WorkoutBlock(
        id = id,
        uuid = "u$id",
        type = "SUPER",
        restSeconds = 90,
        exercises = listOf(a, b),
        rounds = List(rounds) { r -> WorkoutRound(id * 10 + r, listOf(WorkoutSet(reps = 12), WorkoutSet(reps = 10))) },
    )

    private fun List<WorkoutBlock>.shape() = map { block -> block.type to block.exercises.map { it.uuid } }

    @Test
    fun `picked singles become one group where the first was, their sets lined up by copying the shorter one's last`() {
        val blocks = listOf(single(1, benchItem, 10, 8, 8), single(2, squatItem, 5), single(3, flyItem, 15, 12))

        val grouped = blocks.groupExercises(listOf(ExerciseRef(3, 0), ExerciseRef(1, 0)), ELSetType.SUPER, SessionIds(100))

        assertThat(grouped.shape()).containsExactly("SUPER" to listOf("bench-item", "fly-item"), "SINGLE" to listOf("squat-item")).inOrder()
        assertThat(grouped[0].rounds.map { round -> round.sets.map { it.reps } })
            .containsExactly(listOf(10, 15), listOf(8, 12), listOf(8, 12)).inOrder()
        assertThat(grouped[0].restSeconds).isEqualTo(60)
        assertThat(grouped[0].uuid).isNotNull()
    }

    @Test
    fun `an exercise picked out of a superset leaves the other as a single`() {
        val blocks = listOf(superset(1, flyItem, raiseItem, rounds = 2), single(2, squatItem, 5, 5))

        val grouped = blocks.groupExercises(listOf(ExerciseRef(1, 1), ExerciseRef(2, 0)), ELSetType.SUPER, SessionIds(100))

        assertThat(grouped.shape()).containsExactly("SUPER" to listOf("raise-item", "squat-item"), "SINGLE" to listOf("fly-item")).inOrder()
    }

    @Test
    fun `picking the whole of one block only changes its type`() {
        val blocks = listOf(superset(1, flyItem, raiseItem, rounds = 2), single(2, benchItem, 8))

        val changed = blocks.groupExercises(listOf(ExerciseRef(1, 0), ExerciseRef(1, 1)), ELSetType.CIRCUIT, SessionIds(100))
        val dropped = blocks.groupExercises(listOf(ExerciseRef(2, 0)), ELSetType.DROP, SessionIds(100))

        assertThat(changed[0]).isEqualTo(blocks[0].copy(type = "CIRCUIT"))
        assertThat(dropped[1]).isEqualTo(blocks[1].copy(type = "DROP"))
    }

    @Test
    fun `ungrouping takes exercises out as singles right after their group, which keeps the rest`() {
        val blocks = listOf(superset(1, flyItem, raiseItem, rounds = 2), single(2, squatItem, 5))

        val one = blocks.ungroupExercises(listOf(ExerciseRef(1, 0)), SessionIds(100))
        val both = blocks.ungroupExercises(listOf(ExerciseRef(1, 0), ExerciseRef(1, 1)), SessionIds(100))

        assertThat(one.shape()).containsExactly(
            "SINGLE" to listOf("raise-item"), "SINGLE" to listOf("fly-item"), "SINGLE" to listOf("squat-item"),
        ).inOrder()
        assertThat(one[1].rounds.map { it.sets.single().reps }).containsExactly(12, 12)
        assertThat(both.shape()).containsExactly(
            "SINGLE" to listOf("fly-item"), "SINGLE" to listOf("raise-item"), "SINGLE" to listOf("squat-item"),
        ).inOrder()
        assertThat(blocks.ungroupExercises(listOf(ExerciseRef(2, 0)), SessionIds(100))).isEqualTo(blocks)
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
