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
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.controllers.workoutprefill.RealLoadPrefillSourcesUseCase
import com.everlog.data.controllers.workoutsession.RealWorkoutSetPrefill
import com.everlog.data.datastores.exercises.ELExercisesStore
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.workout.ELWorkout
import com.everlog.data.repositories.RealOngoingWorkoutRepository
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.navigator.ELNavigator
import kotlinx.coroutines.Dispatchers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * For workouts started while Remote Config's workout_screen_v2 is on (ELNavigator.workoutActivity).
 */
class WorkoutLogActivity : CommonComposeActivity() {
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
                    loadPrefillSources = RealLoadPrefillSourcesUseCase(Dispatchers.Default),
                    prefill = RealWorkoutSetPrefill(),
                )
            }
        }
    }

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
        if (savedInstanceState == null) {
            // Closes screens that shouldn't stay under a workout
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ELConstants.BROADCAST_WORKOUT_STARTED))
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, WorkoutLogNotificationReceiver.Filter)
        EventBus.getDefault().register(this)
        startNotification()
    }

    // E.g. renamed from its details
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onExerciseModified(event: ELExercisesStore.ELColStoreExerciseModifiedEvent) {
        viewModel.onExerciseChanged(event.item)
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
        EventBus.getDefault().unregister(this)
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

    @Composable
    override fun Content() = WorkoutLogScreen(
        viewModel = viewModel,
        onAddExercise = { ELNavigator(this).openExercisePicker() },
        onOpenExercise = { ELNavigator(this).openExerciseDetails(ExerciseDetailsActivity.Companion.Properties().exercise(it)) },
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
