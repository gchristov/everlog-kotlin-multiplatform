package com.everlog.ui.fragments.home.exercise.info

import com.everlog.data.controllers.exerciseguide.LoadExerciseGuideUseCase
import com.everlog.data.model.exercise.library.ExerciseGuide
import com.everlog.ui.mvvm.CommonViewModel
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

/**
 * The exercise detail's Info tab: the exercise's image, then the library's steps and tips once they
 * load. Users' own exercises aren't in the library, so they skip the read and show only the image,
 * as does a read that fails (e.g. offline with the exercise never read before).
 */
class ExerciseInfoViewModel(
    dispatcher: CoroutineDispatcher,
    private val loadGuide: LoadExerciseGuideUseCase,
    exercise: Exercise,
) : CommonViewModel<ExerciseInfoViewModel.State>(dispatcher, State(exercise = exercise)) {

    // What the tab shows of the exercise it was opened with
    data class Exercise(
        val uuid: String,
        val name: String,
        val imageUrl: String?,
        val own: Boolean,
    )

    data class State(
        val exercise: Exercise,
        val guide: ExerciseGuide = ExerciseGuide(),
    )

    init {
        if (!exercise.own) {
            launchCoroutine {
                loadGuide(exercise.uuid).fold(
                    ifLeft = { Timber.tag(TAG).w(it, "Couldn't load the guide for %s", exercise.uuid) },
                    ifRight = { guide -> setState { copy(guide = guide) } },
                )
            }
        }
    }

    /**
     * The same exercise, edited while the screen is open (a user's own exercise renamed or given a new
     * image). The guide stays: it belongs to the exercise's uuid, which doesn't change.
     */
    fun updateExercise(exercise: Exercise) {
        setState { copy(exercise = exercise) }
    }

    private companion object {
        const val TAG = "ExerciseInfo"
    }
}
