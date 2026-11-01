package com.everlog.android.template.data

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExerciseTemplateCountsTest {

    @Test
    fun `each exercise counts the routines that use it, once per routine`() {
        val push = routine("push", "bench", "dips", "bench")
        val upper = routine("upper", "bench", "row")

        assertThat(exerciseTemplateCounts(listOf(push, upper))).containsExactly("bench", 2, "dips", 1, "row", 1)
    }

    @Test
    fun `no routines, no counts`() {
        assertThat(exerciseTemplateCounts(emptyList())).isEmpty()
    }

    private fun routine(uuid: String, vararg exercises: String) = ELRoutine(
        uuid = uuid,
        name = uuid,
        exerciseGroups = exercises.mapIndexed { index, exercise ->
            ELExerciseGroup(uuid = "$uuid-$index", type = "SINGLE", exercises = mutableListOf(ELRoutineExercise("$uuid-$index-item", ELExercise(uuid = exercise, name = exercise), mutableListOf())))
        }.toMutableList(),
    )
}
