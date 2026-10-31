package com.everlog.data.controllers.templatesession

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.config.AppConfig
import com.everlog.android.common.session.SessionIds
import com.everlog.android.common.session.SetMode
import com.everlog.android.common.session.SetRef
import com.everlog.android.common.session.WorkoutSet
import com.everlog.android.common.session.deleteSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TemplateSessionMapperTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")
    private val fly = ELExercise(uuid = "fly", name = "Fly")
    private val raise = ELExercise(uuid = "raise", name = "Lateral raise")

    @Test
    fun `a template's targets become the values the editor shows`() {
        val routine = routine(group("SINGLE", bench to listOf(ELSet(requiredReps = 10), ELSet(requiredReps = 8), ELSet())))

        val sets = routine.toTemplateBlocks(SessionIds()).single().rounds.map { it.sets.single() }

        assertThat(sets).containsExactly(WorkoutSet(reps = 10), WorkoutSet(reps = 8), WorkoutSet()).inOrder()
    }

    @Test
    fun `a timed target is a time set`() {
        val routine = routine(group("SINGLE", bench to listOf(ELSet(requiredTimeSeconds = 45))))

        val set = routine.toTemplateBlocks(SessionIds()).single().rounds.single().sets.single()

        assertThat(set).isEqualTo(WorkoutSet(mode = SetMode.Time, timeSeconds = 45))
    }

    @Test
    fun `performed values and weights in a template are ignored`() {
        val routine = routine(group("SINGLE", bench to listOf(ELSet(requiredReps = 10, reps = 8, weight = 60f, completedDate = 1L))))

        val set = routine.toTemplateBlocks(SessionIds()).single().rounds.single().sets.single()

        assertThat(set).isEqualTo(WorkoutSet(reps = 10))
    }

    @Test
    fun `a superset comes back as it was, with its uuids, exercises and rest`() {
        val routine = routine(group("SUPER",
            fly to listOf(ELSet(requiredReps = 15), ELSet(requiredReps = 12)),
            raise to listOf(ELSet(requiredTimeSeconds = 30), ELSet(requiredTimeSeconds = 40))))

        val saved = routine.withTemplateBlocks(routine.toTemplateBlocks(SessionIds()), restSeconds = 60)

        val group = saved.exerciseGroups.single()
        assertThat(group.uuid).isEqualTo("group")
        assertThat(group.type).isEqualTo("SUPER")
        assertThat(group.exercises.map { it.uuid }).containsExactly("fly-item", "raise-item").inOrder()
        assertThat(group.exercises.map { it.exercise }).containsExactly(fly, raise).inOrder()
        assertThat(group.exercises[0].sets.map { it.asMap() }).isEqualTo(routine.exerciseGroups.single().exercises[0].sets.map { it.asMap() })
        assertThat(group.exercises[1].sets.map { it.asMap() }).isEqualTo(routine.exerciseGroups.single().exercises[1].sets.map { it.asMap() })
    }

    @Test
    fun `only the set's kind of target is saved, never a weight`() {
        val routine = routine(group("SINGLE", bench to listOf(ELSet(requiredReps = 10))))
        val blocks = routine.toTemplateBlocks(SessionIds()).setExerciseMode(blockId = 1, exercise = 0, mode = SetMode.Time)

        val set = routine.withTemplateBlocks(blocks, restSeconds = 60).exerciseGroups.single().exercises.single().sets.single()

        assertThat(set.asMap()).containsAtLeast("requiredTimeSeconds", DefaultTimeSeconds, "requiredReps", null, "weight", null)
    }

    @Test
    fun `removed sets are left out, and a group left with one exercise is a single`() {
        val routine = routine(group("SUPER",
            fly to listOf(ELSet(requiredReps = 15), ELSet(requiredReps = 12)),
            raise to listOf(ELSet(requiredReps = 10))))
        val blocks = routine.toTemplateBlocks(SessionIds())
        val block = blocks.single()
        val (edited, _) = blocks.deleteSet(SetRef(block.id, block.rounds[0].id, 0))!!

        val group = routine.withTemplateBlocks(edited, restSeconds = 60).exerciseGroups.single()

        assertThat(group.exercises.map { it.exercise }).containsExactly(fly, raise).inOrder()
        assertThat(group.exercises[0].sets.map { it.getRequiredReps() }).containsExactly(12)

        val (onlyFly, _) = edited.deleteSet(SetRef(block.id, block.rounds[0].id, 1))!!
        val single = routine.withTemplateBlocks(onlyFly, restSeconds = 60).exerciseGroups.single()
        assertThat(single.type).isEqualTo("SINGLE")
        assertThat(single.exercises.map { it.exercise }).containsExactly(fly)
    }

    @Test
    fun `every group gets the template's rest`() {
        val routine = routine(group("SINGLE", bench to listOf(ELSet(requiredReps = 5))), group("SINGLE", fly to listOf(ELSet(requiredReps = 12))))

        val saved = routine.withTemplateBlocks(routine.toTemplateBlocks(SessionIds()), restSeconds = 90)

        assertThat(saved.exerciseGroups.map { it.restTimeSeconds }).containsExactly(90, 90)
    }

    @Test
    fun `the template's rest is its first group's, no rest included, or the default without groups`() {
        assertThat(routine(group("SINGLE", bench to listOf(ELSet()), rest = 45)).templateRestSeconds()).isEqualTo(45)
        assertThat(routine(group("SINGLE", bench to listOf(ELSet()), rest = 0)).templateRestSeconds()).isEqualTo(0)
        assertThat(routine().templateRestSeconds()).isEqualTo(AppConfig.configuration.defaultRestTimeSeconds)
    }

    @Test
    fun `saving leaves the routine it was loaded from as it was`() {
        val routine = routine(group("SINGLE", bench to listOf(ELSet(requiredReps = 5))))

        val saved = routine.withTemplateBlocks(emptyList(), restSeconds = 60)

        assertThat(saved.exerciseGroups).isEmpty()
        assertThat(saved.uuid).isEqualTo("routine")
        assertThat(routine.exerciseGroups).hasSize(1)
    }

    private fun group(type: String, vararg exercises: Pair<ELExercise, List<ELSet>>, rest: Int = 60) = ELExerciseGroup(
        uuid = "group",
        type = type,
        exercises = exercises.map { (exercise, sets) -> ELRoutineExercise("${exercise.uuid}-item", exercise, sets.toMutableList()) }.toMutableList(),
        restTimeSeconds = rest,
    )

    private fun routine(vararg groups: ELExerciseGroup) = ELRoutine(uuid = "routine", name = "Push", exerciseGroups = groups.toMutableList())
}
