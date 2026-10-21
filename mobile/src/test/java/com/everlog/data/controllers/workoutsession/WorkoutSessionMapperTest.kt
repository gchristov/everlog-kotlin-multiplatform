package com.everlog.data.controllers.workoutsession

import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

class WorkoutSessionMapperTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")
    private val fly = ELExercise(uuid = "fly", name = "Fly")
    private val raise = ELExercise(uuid = "raise", name = "Lateral raise")

    @Test
    fun `a single exercise's sets become its rounds`() {
        val workout = workout(group("SINGLE", bench to listOf(ELSet(reps = 8, weight = 60f), ELSet(requiredReps = 8))))

        val block = workout.toSessionBlocks(SessionIds()).single()

        assertThat(block.type).isEqualTo("SINGLE")
        assertThat(block.rounds.map { it.sets.single() }).containsExactly(
            WorkoutSet(reps = 8, weightKg = 60f),
            WorkoutSet(requiredReps = 8),
        ).inOrder()
    }

    @Test
    fun `a superset's rounds pair the sets at the same index`() {
        val workout = workout(group("SUPER",
            fly to listOf(ELSet(reps = 15), ELSet(reps = 14)),
            raise to listOf(ELSet(reps = 12), ELSet(reps = 10))))

        val rounds = workout.toSessionBlocks(SessionIds()).single().rounds

        assertThat(rounds.map { round -> round.sets.map { it.reps } }).containsExactly(listOf(15, 12), listOf(14, 10)).inOrder()
    }

    @Test
    fun `an exercise with fewer sets has removed sets in the rounds it's missing from`() {
        val workout = workout(group("SUPER",
            fly to listOf(ELSet(reps = 15), ELSet(reps = 14), ELSet(reps = 13)),
            raise to listOf(ELSet(reps = 12))))

        val rounds = workout.toSessionBlocks(SessionIds()).single().rounds

        assertThat(rounds.map { it.sets[1].removed }).containsExactly(false, true, true).inOrder()
    }

    @Test
    fun `time sets are in time mode, also with only a target time`() {
        val workout = workout(group("SINGLE", bench to listOf(ELSet(timeSeconds = 30), ELSet(requiredTimeSeconds = 40), ELSet(reps = 5))))

        val modes = workout.toSessionBlocks(SessionIds()).single().rounds.map { it.sets.single().mode }

        assertThat(modes).containsExactly(SetMode.Time, SetMode.Time, SetMode.Reps).inOrder()
    }

    @Test
    fun `a done set keeps its dates`() {
        val workout = workout(group("SINGLE", bench to listOf(ELSet(startedDate = 100, completedDate = 200, reps = 8))))

        val set = workout.toSessionBlocks(SessionIds()).single().rounds.single().sets.single()

        assertThat(set.done).isTrue()
        assertThat(set.startedDate).isEqualTo(100)
        assertThat(set.completedDate).isEqualTo(200)
    }

    @Test
    fun `a weight saved as -1 has no weight`() {
        val workout = workout(group("SINGLE", bench to listOf(ELSet(reps = 10, weight = -1f))))

        val set = workout.toSessionBlocks(SessionIds()).single().rounds.single().sets.single()

        assertThat(set.weightKg).isNull()
    }

    @Test
    fun `mapping back gives the same workout`() {
        val workout = workout(
            group("SINGLE", bench to listOf(
                ELSet(startedDate = 100, completedDate = 200, requiredReps = 8, reps = 10, weight = 60f),
                ELSet(reps = 10, suggestedReps = 10),
                ELSet(requiredTimeSeconds = 45, timeSeconds = 40, weight = 12.5f, suggestedWeight = 12.5f, suggestedTimeSeconds = 40))),
            group("SUPER",
                fly to listOf(ELSet(reps = 15), ELSet(reps = 14), ELSet(reps = 13)),
                raise to listOf(ELSet(reps = 12))))

        val mapped = workout.withSessionBlocks(workout.toSessionBlocks(SessionIds()))

        // ELRoutineExercise's equals only compares uuids, so compare what's saved and each set too
        assertThat(mapped.asMap()).isEqualTo(workout.asMap())
        assertThat(sets(mapped)).isEqualTo(sets(workout))
    }

    @Test
    fun `mapping back keeps the workout's own fields`() {
        val workout = workout(group("SINGLE", bench to listOf(ELSet(reps = 8)))).apply {
            uuid = "workout"
            createdDate = 1000
            fromRoutine = true
            routine?.name = "Push day"
        }

        val mapped = workout.withSessionBlocks(emptyList())

        assertThat(mapped.uuid).isEqualTo("workout")
        assertThat(mapped.createdDate).isEqualTo(1000)
        assertThat(mapped.fromRoutine).isTrue()
        assertThat(mapped.getName()).isEqualTo("Push day")
        assertThat(mapped.getExerciseGroups()).isEmpty()
    }

    @Test
    fun `only the set's mode's values are saved`() {
        val block = block(WorkoutSet(mode = SetMode.Time, reps = 8, timeSeconds = 30, requiredReps = 8, requiredTimeSeconds = 45))

        val set = workout().withSessionBlocks(listOf(block)).getExerciseGroups().single().exercises.single().sets.single()

        assertThat(set.isRepBased()).isFalse()
        assertThat(set.getTimeSeconds()).isEqualTo(30)
        assertThat(set.getRequiredTimeSeconds()).isEqualTo(45)
    }

    @Test
    fun `the ongoing workout keeps a removed set between two sets, so its rounds line up again`() {
        val workout = workout(group("SUPER",
            fly to listOf(ELSet(reps = 15), ELSet(reps = 14), ELSet(reps = 13)),
            raise to listOf(ELSet(reps = 12), ELSet(reps = 10), ELSet(reps = 8))))
        val blocks = workout.toSessionBlocks(SessionIds())
        val removed = blocks.updateSet(SetRef(blocks[0].id, blocks[0].rounds[1].id, 1)) { it.copy(removed = true) }

        // Through the saved JSON, as when the app is killed and the workout resumed
        val saved = Gson().fromJson(Gson().toJson(workout.withSessionBlocks(removed)), ELWorkout::class.java)
        val reloaded = saved.toSessionBlocks(SessionIds())

        assertThat(reloaded.single().rounds.map { round -> round.sets.map { if (it.removed) null else it.reps } })
            .containsExactly(listOf(15, 12), listOf(14, null), listOf(13, 8)).inOrder()
    }

    @Test
    fun `the finished workout leaves removed sets out`() {
        val workout = workout(group("SUPER",
            fly to listOf(ELSet(reps = 15), ELSet(reps = 14)),
            raise to listOf(ELSet(reps = 12), ELSet(reps = 10))))
        val blocks = workout.toSessionBlocks(SessionIds())
        val removed = blocks.updateSet(SetRef(blocks[0].id, blocks[0].rounds[0].id, 1)) { it.copy(removed = true) }

        val exercises = workout.withSessionBlocks(removed, finished = true).getExerciseGroups().single().exercises

        assertThat(exercises.map { exercise -> exercise.sets.map { it.getReps() } }).containsExactly(listOf(15, 14), listOf(10)).inOrder()
    }

    @Test
    fun `the finished workout leaves out an exercise with every set removed`() {
        val workout = workout(group("SUPER", fly to listOf(ELSet(reps = 15)), raise to listOf(ELSet(reps = 12))))
        val blocks = workout.toSessionBlocks(SessionIds())
        val removed = blocks.updateSet(SetRef(blocks[0].id, blocks[0].rounds[0].id, 1)) { it.copy(removed = true) }

        val group = workout.withSessionBlocks(removed, finished = true).getExerciseGroups().single()

        assertThat(group.exercises.map { it.uuid }).containsExactly("fly")
        assertThat(group.type).isEqualTo("SINGLE")
    }

    @Test
    fun `the next set for the notification skips removed sets`() {
        val workout = workout(group("SUPER",
            fly to listOf(ELSet(completedDate = 100, reps = 15), ELSet(reps = 14)),
            raise to listOf(ELSet(reps = 12, removed = true), ELSet(reps = 10))))

        val next = workout.getNextIncompleteState()!!

        assertThat(next.setIndex to next.exerciseIndex).isEqualTo(1 to 0)
    }

    private fun group(type: String, vararg exercises: Pair<ELExercise, List<ELSet>>) = ELExerciseGroup(
        uuid = "group-$type",
        type = type,
        exercises = exercises.map { (exercise, sets) -> ELRoutineExercise(exercise.uuid, exercise, sets.toMutableList()) }.toMutableList(),
    )

    private fun workout(vararg groups: ELExerciseGroup) = ELWorkout(routine = ELRoutine(exerciseGroups = groups.toMutableList()))

    private fun sets(workout: ELWorkout) = workout.getExerciseGroups().flatMap { group -> group.exercises.map { it.sets } }

    private fun block(set: WorkoutSet) = WorkoutBlock(1, "group", "SINGLE", 60, listOf(WorkoutExercise("bench", bench)), listOf(WorkoutRound(2, listOf(set))))
}
