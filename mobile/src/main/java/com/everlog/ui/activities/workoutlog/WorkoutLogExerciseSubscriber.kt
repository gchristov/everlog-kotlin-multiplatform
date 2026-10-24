package com.everlog.ui.activities.workoutlog

import com.everlog.data.datastores.exercises.ELExercisesStore
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Exercise changes from the exercises store, e.g. one renamed from its details.
 *
 * Registered on EventBus instead of the activity: EventBus reads the methods of every superclass outside `androidx.`,
 * and R8 renames androidx's ComponentActivity in release builds, so it would read
 * `onPictureInPictureUiStateChanged(PictureInPictureUiState)` and crash below Android 12.
 */
internal class WorkoutLogExerciseSubscriber(private val viewModel: WorkoutLogViewModel) {

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onExerciseModified(event: ELExercisesStore.ELColStoreExerciseModifiedEvent) {
        viewModel.onExerciseChanged(event.item)
    }
}
