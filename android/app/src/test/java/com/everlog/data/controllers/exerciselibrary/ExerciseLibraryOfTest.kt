package com.everlog.data.controllers.exerciselibrary

import com.everlog.android.common.kotlin.Load
import com.everlog.data.controllers.starterroutines.ExerciseLibraryEmptyException
import com.everlog.data.model.exercise.library.ExerciseLibrary
import com.everlog.data.model.exercise.library.ExerciseRegistry
import com.everlog.data.model.exercise.library.LibraryExercise
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException

class ExerciseLibraryOfTest {

    private val bench = LibraryExercise(uuid = "bench", name = "Barbell Bench Press", family = "bench_press")
    private val mine = LibraryExercise(uuid = "mine", name = "My Press", own = true)
    private val registry = ExerciseRegistry(families = listOf(ExerciseRegistry.Family(ExerciseRegistry.Entry(id = "bench_press", name = "Bench press"), primaryMuscles = emptyList(), combination = false)))
    private val counts = mapOf("bench" to 2)
    private val lastDone = mapOf("bench" to 10L)

    @Test
    fun `the global exercises, then the user's own, with the registry, counts and last done`() {
        val load = libraryOf(global = Load.Ready(listOf(bench)), own = Load.Ready(listOf(mine)), registry = Load.Ready(registry))

        assertThat(load).isEqualTo(Load.Ready(ExerciseLibrary(listOf(bench, mine), registry, counts, lastDone)))
    }

    @Test
    fun `waits until every part has answered`() {
        assertThat(libraryOf(global = Load.Loading)).isEqualTo(Load.Loading)
        assertThat(libraryOf(own = Load.Loading)).isEqualTo(Load.Loading)
        assertThat(libraryOf(registry = Load.Loading)).isEqualTo(Load.Loading)
    }

    @Test
    fun `without the registry or the user's own exercises, the global ones still show`() {
        val load = libraryOf(own = Load.Unavailable(IOException("offline")), registry = Load.Ready(null))

        assertThat(load).isEqualTo(Load.Ready(ExerciseLibrary(listOf(bench), ExerciseRegistry(), counts, lastDone)))
    }

    @Test
    fun `unavailable when the global exercises are, or the server has none`() {
        val offline = Load.Unavailable(IOException("offline"))

        assertThat(libraryOf(global = offline)).isEqualTo(offline)
        assertThat((libraryOf(global = Load.Ready(emptyList())) as Load.Unavailable).error).isInstanceOf(ExerciseLibraryEmptyException::class.java)
    }

    @Test
    fun `a family the registry doesn't know counts as none`() {
        val unknown = bench.copy(family = "floor_press")

        val load = libraryOf(global = Load.Ready(listOf(unknown))) as Load.Ready

        assertThat(load.value.exercises.single().family).isNull()
    }

    private fun libraryOf(
        global: Load<List<LibraryExercise>> = Load.Ready(listOf(bench)),
        own: Load<List<LibraryExercise>> = Load.Ready(emptyList()),
        registry: Load<ExerciseRegistry?> = Load.Ready(this.registry),
    ) = exerciseLibraryOf(global, own, registry, counts, lastDone)
}
