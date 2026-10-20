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
import com.everlog.data.datastores.exercises.ELExercisesStore
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.workout.ELWorkout
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.navigator.ELNavigator
import kotlinx.coroutines.Dispatchers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

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
                    alerts = RealWorkoutLogAlerts(applicationContext),
                    notification = RealWorkoutLogNotification(ELNavigator(applicationContext)),
                )
            }
        }
    }

    private val notificationReceiver by lazy { WorkoutLogNotificationReceiver(viewModel) }

    // The notification needs these, as on the old screen. Without them it asks to have them turned on in Settings.
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
            // Closes screens that shouldn't stay under a workout, as the old screen does
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ELConstants.BROADCAST_WORKOUT_STARTED))
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, WorkoutLogNotificationReceiver.Filter)
        EventBus.getDefault().register(this)
        startNotification()
    }

    // An exercise edited meanwhile, e.g. renamed from its details
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
