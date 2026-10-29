package com.everlog.ui.activities.workoutlog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.R
import com.everlog.config.FeedbackSource
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.controllers.workoutprefill.RealLoadPrefillSourcesUseCase
import com.everlog.data.controllers.workoutsession.RealWorkoutSetPrefill
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.workout.ELWorkout
import com.everlog.data.repositories.RealExerciseRepository
import com.everlog.data.repositories.RealWorkoutRepository
import com.everlog.data.sync.AppDataSync
import com.everlog.data.repositories.RealOngoingWorkoutRepository
import com.everlog.ui.activities.base.SignedInComposeActivity
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.ui.components.exercisetable.ExerciseTab
import com.everlog.ui.navigator.ELNavigator
import kotlinx.coroutines.Dispatchers
import com.everlog.ui.navigator.feedbackFormLauncher

/**
 * For workouts started while Remote Config's workout_screen_v2 is on (ELNavigator.workoutActivity).
 */
class WorkoutLogActivity : SignedInComposeActivity() {
    private val viewModel by viewModels<WorkoutLogViewModel> {
        viewModelFactory {
            initializer {
                val repository = RealOngoingWorkoutRepository()
                @Suppress("DEPRECATION")
                val launched = intent.getSerializableExtra(ELConstants.EXTRA_WORKOUT) as ELWorkout
                val loaded = WorkoutLogViewModel.load(launched, repository)
                WorkoutLogViewModel(
                    dispatcher = Dispatchers.Main,
                    workout = loaded.workout,
                    resumed = loaded.resumed,
                    fromPlan = intent.getBooleanExtra(ELConstants.EXTRA_WORKOUT_FROM_PLAN, false),
                    repository = repository,
                    settings = RealWorkoutLogSettings(),
                    analytics = RealWorkoutLogAnalytics(),
                    alerts = RealWorkoutLogAlerts(applicationContext),
                    notification = RealWorkoutLogNotification(ELNavigator(applicationContext)),
                    loadPrefillSources = RealLoadPrefillSourcesUseCase(Dispatchers.Default, RealWorkoutRepository(Dispatchers.IO, AppDataSync.instance)),
                    prefill = RealWorkoutSetPrefill(),
                    ownExerciseUpdates = RealExerciseRepository(Dispatchers.IO, AppDataSync.instance)::ownExerciseUpdates,
                )
            }
        }
    }

    override val requiredExtras = listOf(ELConstants.EXTRA_WORKOUT)

    private val notificationReceiver by lazy { WorkoutLogNotificationReceiver(viewModel) }

    // The notification needs these
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) {
            viewModel.onScreenStarted()
        } else {
            ELNavigator(this).promptForAppSettings(R.string.workout_error_no_permissions)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isOpened) return
        if (savedInstanceState == null) {
            // Closes screens that shouldn't stay under a workout
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ELConstants.BROADCAST_WORKOUT_STARTED))
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, WorkoutLogNotificationReceiver.Filter)
        startNotification()
    }

    override fun onDestroy() {
        if (!isOpened) {
            super.onDestroy()
            return
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
        // The notification stays while the screen is only being rebuilt, e.g. for a dark mode change: the new screen
        // starts it again
        if (!isChangingConfigurations) viewModel.onScreenClosed()
        super.onDestroy()
    }

    private fun startNotification() {
        val missing = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) viewModel.onScreenStarted() else permissions.launch(missing.toTypedArray())
    }

    private val sendFeedback by lazy { feedbackFormLauncher(FeedbackSource.WORKOUT_MENU) }

    @Composable
    override fun Content() = WorkoutLogScreen(
        viewModel = viewModel,
        onAddExercise = { ELNavigator(this).openExercisePicker() },
        onOpenExercise = { exercise, tab ->
            val type = when (tab) {
                ExerciseTab.Info -> ExerciseDetailsActivity.Companion.Type.INFO
                ExerciseTab.History -> ExerciseDetailsActivity.Companion.Type.HISTORY
            }
            ELNavigator(this).openExerciseDetails(ExerciseDetailsActivity.Companion.Properties().exercise(exercise).type(type))
        },
        onOpenSetSuggestions = { ELNavigator(this).openMuscleGoal() },
        onSendFeedback = sendFeedback,
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
