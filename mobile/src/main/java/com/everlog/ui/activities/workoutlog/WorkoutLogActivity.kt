package com.everlog.ui.activities.workoutlog

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.workout.ELWorkout
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.navigator.ELNavigator
import kotlinx.coroutines.Dispatchers

/**
 * The workout screen in Compose (TAS-455), for workouts started while Remote Config's workout_screen_v2 is on (see
 * ELNavigator.workoutActivity). Opened with the workout as [ELConstants.EXTRA_WORKOUT], like WorkoutActivity.
 */
class WorkoutLogActivity : CommonComposeActivity() {
    private val viewModel by viewModels<WorkoutLogViewModel> {
        viewModelFactory {
            initializer {
                val store = RealWorkoutLogStore()
                @Suppress("DEPRECATION")
                val launched = intent.getSerializableExtra(ELConstants.EXTRA_WORKOUT) as ELWorkout
                WorkoutLogViewModel(
                    dispatcher = Dispatchers.Main,
                    workout = WorkoutLogViewModel.load(launched, store),
                    fromPlan = intent.getBooleanExtra(ELConstants.EXTRA_WORKOUT_FROM_PLAN, false),
                    store = store,
                    settings = RealWorkoutLogSettings(),
                    analytics = RealWorkoutLogAnalytics(),
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            // Closes screens that shouldn't stay under a workout, as the old screen does
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ELConstants.BROADCAST_WORKOUT_STARTED))
        }
    }

    @Composable
    override fun Content() = WorkoutLogScreen(
        viewModel = viewModel,
        onAddExercise = { ELNavigator(this).openExercisePicker() },
        onExit = ::exit,
    )

    @Deprecated("The navigator opens the exercise picker with startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ELActivityRequestCodes.REQUEST_PICK_EXERCISES && resultCode == RESULT_OK) {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            (data?.getSerializableExtra(ELConstants.EXTRA_EXERCISE_GROUPS) as? List<ELExerciseGroup>)?.let(viewModel::onExercisesPicked)
        }
    }

    private fun exit(exit: WorkoutLogExit) {
        if (exit is WorkoutLogExit.Finished) {
            ELNavigator(this).openWorkoutDetails(exit.workout, true, intent.getBooleanExtra(ELConstants.EXTRA_WORKOUT_FROM_PLAN, false))
        }
        finish()
    }
}
