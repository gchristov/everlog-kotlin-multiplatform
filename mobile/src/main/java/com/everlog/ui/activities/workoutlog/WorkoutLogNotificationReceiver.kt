package com.everlog.ui.activities.workoutlog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.everlog.constants.ELConstants
import com.everlog.data.model.workout.ELWorkoutState
import com.everlog.services.workout.WorkoutService

/**
 * The notification's buttons, which WorkoutService sends as local broadcasts, each with the set it shows.
 */
internal class WorkoutLogNotificationReceiver(private val viewModel: WorkoutLogViewModel) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == WorkoutService.BROADCAST_REQUEST_TIMER_REST_STOP) {
            viewModel.onNotificationRestStop()
            return
        }
        @Suppress("DEPRECATION")
        val position = intent?.getSerializableExtra(ELConstants.EXTRA_WORKOUT_STATE) as? ELWorkoutState ?: return
        when (intent.action) {
            WorkoutService.BROADCAST_REQUEST_COMPLETE_SET -> viewModel.onNotificationMainAction(position)
            WorkoutService.BROADCAST_REQUEST_INCREASE_WEIGHT -> viewModel.onNotificationWeight(position, 1)
            WorkoutService.BROADCAST_REQUEST_DECREASE_WEIGHT -> viewModel.onNotificationWeight(position, -1)
            WorkoutService.BROADCAST_REQUEST_INCREASE_REPS -> viewModel.onNotificationReps(position, 1)
            WorkoutService.BROADCAST_REQUEST_DECREASE_REPS -> viewModel.onNotificationReps(position, -1)
            WorkoutService.BROADCAST_REQUEST_INCREASE_TIME -> viewModel.onNotificationTime(position, 1)
            WorkoutService.BROADCAST_REQUEST_DECREASE_TIME -> viewModel.onNotificationTime(position, -1)
            WorkoutService.BROADCAST_REQUEST_TIMER_EXERCISE_START -> viewModel.onNotificationSetTimer(position, start = true)
            WorkoutService.BROADCAST_REQUEST_TIMER_EXERCISE_STOP -> viewModel.onNotificationSetTimer(position, start = false)
        }
    }

    companion object {
        val Filter = IntentFilter().apply {
            addAction(WorkoutService.BROADCAST_REQUEST_COMPLETE_SET)
            addAction(WorkoutService.BROADCAST_REQUEST_INCREASE_WEIGHT)
            addAction(WorkoutService.BROADCAST_REQUEST_DECREASE_WEIGHT)
            addAction(WorkoutService.BROADCAST_REQUEST_INCREASE_REPS)
            addAction(WorkoutService.BROADCAST_REQUEST_DECREASE_REPS)
            addAction(WorkoutService.BROADCAST_REQUEST_INCREASE_TIME)
            addAction(WorkoutService.BROADCAST_REQUEST_DECREASE_TIME)
            addAction(WorkoutService.BROADCAST_REQUEST_TIMER_REST_STOP)
            addAction(WorkoutService.BROADCAST_REQUEST_TIMER_EXERCISE_START)
            addAction(WorkoutService.BROADCAST_REQUEST_TIMER_EXERCISE_STOP)
        }
    }
}
