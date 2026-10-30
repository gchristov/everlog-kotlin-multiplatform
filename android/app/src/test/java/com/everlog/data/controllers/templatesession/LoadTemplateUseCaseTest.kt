package com.everlog.data.controllers.templatesession

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.kotlin.Load
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.testutil.FakeExerciseRepository
import com.everlog.testutil.FakeRoutineRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test

class LoadTemplateUseCaseTest {

    private val error = IllegalStateException("offline")
    private val routine = ELRoutine(
        uuid = "push",
        name = "Push",
        exerciseGroups = mutableListOf(
            ELExerciseGroup(uuid = "g1", type = "SINGLE", exercises = mutableListOf(item("bench", "Bench press (old name)"))),
            ELExerciseGroup(uuid = "g2", type = "SINGLE", exercises = mutableListOf(item("mine", "My curl (old name)"))),
            ELExerciseGroup(uuid = "g3", type = "SINGLE", exercises = mutableListOf(item("deleted", "Deleted exercise"))),
        ),
    )
    private val library = listOf(LibraryExercise(uuid = "bench", name = "Barbell Bench Press"))
    private val own = listOf(LibraryExercise(uuid = "mine", name = "My curl"))

    @Test
    fun `exercises show as the library and the user's own exercises have them now, others as recorded`() {
        val template = templateOf(Load.Ready(routine), Load.Ready(library), Load.Ready(own))

        assertThat(names(template)).containsExactly("Barbell Bench Press", "My curl", "Deleted exercise").inOrder()
        assertThat((template as Load.Ready).value.exerciseGroups[0].exercises.single().sets.single().getRequiredReps()).isEqualTo(10)
    }

    @Test
    fun `the cached routine isn't changed`() {
        templateOf(Load.Ready(routine), Load.Ready(library), Load.Ready(own))

        assertThat(routine.exerciseGroups[0].exercises.single().exercise?.name).isEqualTo("Bench press (old name)")
    }

    @Test
    fun `exercises that can't be read leave the names as recorded`() {
        val template = templateOf(Load.Ready(routine), Load.Unavailable(error), Load.Unavailable(error))

        assertThat(names(template)).containsExactly("Bench press (old name)", "My curl (old name)", "Deleted exercise").inOrder()
    }

    @Test
    fun `it waits for the routine and the exercises`() {
        assertThat(templateOf(Load.Loading, Load.Ready(library), Load.Ready(own))).isEqualTo(Load.Loading)
        assertThat(templateOf(Load.Ready(routine), Load.Loading, Load.Ready(own))).isEqualTo(Load.Loading)
        assertThat(templateOf(Load.Ready(routine), Load.Ready(library), Load.Loading)).isEqualTo(Load.Loading)
    }

    @Test
    fun `a routine that can't be read, or doesn't exist, is unavailable`() {
        assertThat(templateOf(Load.Unavailable(error), Load.Ready(library), Load.Ready(own))).isEqualTo(Load.Unavailable(error))
        val missing = templateOf(Load.Ready(null), Load.Ready(library), Load.Ready(own))
        assertThat((missing as Load.Unavailable).error).isInstanceOf(TemplateNotFoundException::class.java)
    }

    @Test
    fun `the use case joins the repositories' routine and exercises`() = runBlocking<Unit> {
        val useCase = RealLoadTemplateUseCase(
            Dispatchers.Unconfined,
            FakeRoutineRepository(routine = flowOf(Load.Ready(routine))),
            FakeExerciseRepository(),
        )

        assertThat(names(useCase("push").first())).containsExactly("Bench press (old name)", "My curl (old name)", "Deleted exercise").inOrder()
    }

    private fun item(uuid: String, name: String) =
        ELRoutineExercise("$uuid-item", ELExercise(uuid = uuid, name = name), mutableListOf(ELSet(requiredReps = 10)))

    private fun names(template: Load<ELRoutine>) =
        (template as Load.Ready).value.exerciseGroups.map { it.exercises.single().exercise?.name }
}
