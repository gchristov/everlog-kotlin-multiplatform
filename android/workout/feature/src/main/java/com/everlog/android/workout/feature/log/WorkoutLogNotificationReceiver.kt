package com.everlog.android.workout.feature.log

import com.everlog.android.workout.data.notification.WorkoutBroadcasts
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.everlog.android.common.data.model.workout.ELWorkoutState

/**
 * The notification's buttons, which WorkoutService sends as local broadcasts, each with the set it shows.
 */
internal class WorkoutLogNotificationReceiver(private val viewModel: WorkoutLogViewModel) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == WorkoutBroadcasts.REQUEST_TIMER_REST_STOP) {
            viewModel.onNotificationRestStop()
            return
        }
        @Suppress("DEPRECATION")
        val position = intent?.getSerializableExtra(WorkoutBroadcasts.EXTRA_WORKOUT_STATE) as? ELWorkoutState ?: return
        when (intent.action) {
            WorkoutBroadcasts.REQUEST_COMPLETE_SET -> viewModel.onNotificationMainAction(position)
            WorkoutBroadcasts.REQUEST_INCREASE_WEIGHT -> viewModel.onNotificationWeight(position, 1)
            WorkoutBroadcasts.REQUEST_DECREASE_WEIGHT -> viewModel.onNotificationWeight(position, -1)
            WorkoutBroadcasts.REQUEST_INCREASE_REPS -> viewModel.onNotificationReps(position, 1)
            WorkoutBroadcasts.REQUEST_DECREASE_REPS -> viewModel.onNotificationReps(position, -1)
            WorkoutBroadcasts.REQUEST_INCREASE_TIME -> viewModel.onNotificationTime(position, 1)
            WorkoutBroadcasts.REQUEST_DECREASE_TIME -> viewModel.onNotificationTime(position, -1)
            WorkoutBroadcasts.REQUEST_TIMER_EXERCISE_STOP -> viewModel.onNotificationStopTimer(position)
        }
    }

    companion object {
        val Filter = IntentFilter().apply {
            addAction(WorkoutBroadcasts.REQUEST_COMPLETE_SET)
            addAction(WorkoutBroadcasts.REQUEST_INCREASE_WEIGHT)
            addAction(WorkoutBroadcasts.REQUEST_DECREASE_WEIGHT)
            addAction(WorkoutBroadcasts.REQUEST_INCREASE_REPS)
            addAction(WorkoutBroadcasts.REQUEST_DECREASE_REPS)
            addAction(WorkoutBroadcasts.REQUEST_INCREASE_TIME)
            addAction(WorkoutBroadcasts.REQUEST_DECREASE_TIME)
            addAction(WorkoutBroadcasts.REQUEST_TIMER_REST_STOP)
            addAction(WorkoutBroadcasts.REQUEST_TIMER_EXERCISE_STOP)
        }
    }
}
