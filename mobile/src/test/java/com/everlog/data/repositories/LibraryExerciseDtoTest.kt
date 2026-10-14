package com.everlog.data.repositories

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LibraryExerciseDtoTest {

    @Test
    fun `instructions and tips come across as stored`() {
        val dto = LibraryExerciseDto(name = "Barbell Bench Press", instructions = listOf("Lie on the bench.", "Press."), tips = listOf("Feet flat."))

        val exercise = dto.toLibraryExercise(documentId = "bench", own = false)!!

        assertThat(exercise.instructions).containsExactly("Lie on the bench.", "Press.").inOrder()
        assertThat(exercise.tips).containsExactly("Feet flat.")
    }

    @Test
    fun `an exercise stored without them has none, like users' own`() {
        val exercise = LibraryExerciseDto(name = "Zorbex Press").toLibraryExercise(documentId = "own", own = true)!!

        assertThat(exercise.instructions).isEmpty()
        assertThat(exercise.tips).isEmpty()
    }
}
