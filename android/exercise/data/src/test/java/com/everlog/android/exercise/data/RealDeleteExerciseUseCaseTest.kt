package com.everlog.android.exercise.data

import android.net.Uri
import arrow.core.Either
import com.everlog.android.auth.testfixtures.FakeUserSession
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.exercise.testfixtures.FakeExerciseRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

class RealDeleteExerciseUseCaseTest {

    private val exercise = ELExercise(uuid = "own", name = "Landmine press")
    private val repository = FakeExerciseRepository()
    private val images = FakeImageStorage()

    @Test
    fun `deletes the exercise and then its image`() {
        val result = delete(FakeUserSession())

        assertThat(result.isRight()).isTrue()
        assertThat(repository.deletedExercises).containsExactly("own")
        assertThat(images.deleted).containsExactly("own" to "user")
    }

    @Test
    fun `a rejected delete keeps the image`() {
        repository.deleteResult = Either.Left(IllegalStateException("rejected"))

        val result = delete(FakeUserSession())

        assertThat(result.isLeft()).isTrue()
        assertThat(images.deleted).isEmpty()
    }

    @Test
    fun `without a signed-in user nothing is deleted`() {
        val result = delete(FakeUserSession(signedIn = null))

        assertThat(result.isLeft()).isTrue()
        assertThat(repository.deletedExercises).isEmpty()
        assertThat(images.deleted).isEmpty()
    }

    @Test
    fun `an exercise without a uuid isn't deleted`() {
        val result = runBlocking { RealDeleteExerciseUseCase(repository, images, FakeUserSession())(ELExercise(name = "No id")) }

        assertThat(result.isLeft()).isTrue()
        assertThat(repository.deletedExercises).isEmpty()
    }

    private fun delete(session: FakeUserSession) = runBlocking { RealDeleteExerciseUseCase(repository, images, session)(exercise) }

    private class FakeImageStorage : ExerciseImageStorage {
        val deleted = mutableListOf<Pair<String?, String>>()

        override fun resumePendingUploads() = Unit

        override fun uploadExerciseImage(filePath: Uri, exercise: ELExercise, userId: String) = Unit

        override fun deleteExerciseImage(exercise: ELExercise, userId: String) {
            deleted += exercise.uuid to userId
        }
    }
}
