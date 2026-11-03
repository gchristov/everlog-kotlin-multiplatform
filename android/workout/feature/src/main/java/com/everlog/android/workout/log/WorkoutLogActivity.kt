package com.everlog.android.workout.log

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
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.MvpBroadcasts
import com.everlog.android.common.mvp.activity.SignedInComposeActivity
import com.everlog.android.common.navigation.AppSettingsPrompt
import com.everlog.android.common.navigation.ExerciseDetails
import com.everlog.android.common.navigation.ExercisePicker
import com.everlog.android.common.navigation.FeedbackForm
import com.everlog.android.common.navigation.FeedbackSource
import com.everlog.android.common.navigation.SetSuggestions
import com.everlog.android.common.navigation.WorkoutDetails
import com.everlog.android.common.navigation.launcher
import com.everlog.android.common.navigation.openForResult
import com.everlog.android.workout.R
import com.everlog.android.workout.WorkoutExtras
import com.everlog.android.workout.WorkoutGraph
import com.everlog.android.workout.data.prefill.RealWorkoutSetPrefill
import kotlinx.coroutines.Dispatchers

class WorkoutLogActivity : SignedInComposeActivity() {
    private val graph by lazy { appGraph<WorkoutGraph>() }
    private val navigator by lazy { graph.navigator }

    private val viewModel by viewModels<WorkoutLogViewModel> {
        viewModelFactory {
            initializer {
                val repository = graph.ongoingWorkoutRepository
                @Suppress("DEPRECATION")
                val launched = intent.getSerializableExtra(WorkoutExtras.WORKOUT) as ELWorkout
                val loaded = WorkoutLogViewModel.load(launched, repository)
                WorkoutLogViewModel(
                    dispatcher = Dispatchers.Main,
                    workout = loaded.workout,
                    resumed = loaded.resumed,
                    fromPlan = intent.getBooleanExtra(WorkoutExtras.FROM_PLAN, false),
                    repository = repository,
                    settings = RealWorkoutLogSettings(graph.userSettings),
                    analytics = graph.workoutLogAnalytics,
                    alerts = graph.workoutLogAlerts,
                    notification = graph.workoutLogNotification,
                    loadPrefillSources = graph.loadPrefillSources,
                    prefill = RealWorkoutSetPrefill(graph.userSettings),
                    ownExerciseUpdates = graph.exerciseRepository::ownExerciseUpdates,
                )
            }
        }
    }

    override val requiredExtras = listOf(WorkoutExtras.WORKOUT)

    private val notificationReceiver by lazy { WorkoutLogNotificationReceiver(viewModel) }

    // The notification needs these
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) {
            viewModel.onScreenStarted()
        } else {
            navigator.open(this, AppSettingsPrompt(DesignR.string.workout_error_no_permissions))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isOpened) return
        if (savedInstanceState == null) {
            // Closes screens that shouldn't stay under a workout
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(MvpBroadcasts.WORKOUT_STARTED))
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

    private val sendFeedback by lazy { navigator.launcher(this, FeedbackForm(FeedbackSource.WORKOUT_MENU)) }

    @Composable
    override fun Content() = WorkoutLogScreen(
        viewModel = viewModel,
        onAddExercise = { navigator.openForResult(this, ExercisePicker) },
        onOpenExercise = { exercise, tab -> navigator.open(this, ExerciseDetails(exercise, tab)) },
        onOpenSetSuggestions = { navigator.open(this, SetSuggestions) },
        onSendFeedback = sendFeedback,
        onExit = ::exit,
    )

    @Deprecated("The navigator opens the exercise picker with startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ExercisePicker.result(requestCode, resultCode, data)?.let(viewModel::onExercisesPicked)
    }

    private fun exit(exit: WorkoutLogExit) {
        if (exit is WorkoutLogExit.Finished) {
            navigator.open(this, WorkoutDetails(exit.workout, justFinished = true, fromPlan = intent.getBooleanExtra(WorkoutExtras.FROM_PLAN, false)))
        }
        finish()
    }
}
