package com.everlog.services.workout

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.BuildConfig
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.data.model.workout.ELWorkout
import com.everlog.data.model.workout.ELWorkoutState
import com.everlog.managers.ELNotificationManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.services.BaseService
import java.util.Random

class WorkoutService : BaseService() {

    private var NOTIFICATION_ID = -1

    // State

    private var mWorkout: ELWorkout? = null
    private var mNextState: ELWorkoutState? = null
    private var mRestTimer: WorkoutNotificationState.RestTimer? = null

    private var mReceiver: BroadcastReceiver? = null

    companion object {
        internal const val ACTION_OPEN_WORKOUT = "ACTION_OPEN_WORKOUT"
        internal const val ACTION_DECREASE_WEIGHT = "ACTION_DECREASE_WEIGHT"
        internal const val ACTION_INCREASE_WEIGHT = "ACTION_INCREASE_WEIGHT"
        internal const val ACTION_DECREASE_REPS = "ACTION_DECREASE_REPS"
        internal const val ACTION_INCREASE_REPS = "ACTION_INCREASE_REPS"
        internal const val ACTION_TIMER_EXERCISE_START = "ACTION_TIMER_EXERCISE_START"
        internal const val ACTION_TIMER_EXERCISE_STOP = "ACTION_TIMER_EXERCISE_STOP"
        internal const val ACTION_TIMER_REST_STOP = "ACTION_TIMER_REST_STOP"
        internal const val ACTION_NEXT = "ACTION_NEXT"

        const val ACTION_SERVICE_START = "ACTION_SERVICE_START"
        const val ACTION_SERVICE_STOP = "ACTION_SERVICE_STOP"

        // Requests that the workout activity makes to the service.
        const val BROADCAST_SERVICE_SET_UPDATED = BuildConfig.APPLICATION_ID + ".BROADCAST_SERVICE_SET_UPDATED"
        const val BROADCAST_SERVICE_SHOW_REST_TIMER = BuildConfig.APPLICATION_ID + ".BROADCAST_SERVICE_SHOW_REST_TIMER"

        // Requests that the service makes to the workout activity.
        const val BROADCAST_REQUEST_COMPLETE_SET = BuildConfig.APPLICATION_ID + ".BROADCAST_REQUEST_NEXT_SET"
        const val BROADCAST_REQUEST_INCREASE_WEIGHT = BuildConfig.APPLICATION_ID + ".BROADCAST_REQUEST_INCREASE_WEIGHT"
        const val BROADCAST_REQUEST_DECREASE_WEIGHT = BuildConfig.APPLICATION_ID + ".BROADCAST_REQUEST_DECREASE_WEIGHT"
        const val BROADCAST_REQUEST_INCREASE_REPS = BuildConfig.APPLICATION_ID + ".BROADCAST_REQUEST_INCREASE_REPS"
        const val BROADCAST_REQUEST_DECREASE_REPS = BuildConfig.APPLICATION_ID + ".BROADCAST_REQUEST_DECREASE_REPS"
        const val BROADCAST_REQUEST_TIMER_EXERCISE_START = BuildConfig.APPLICATION_ID + ".BROADCAST_REQUEST_TIMER_EXERCISE_START"
        const val BROADCAST_REQUEST_TIMER_EXERCISE_STOP = BuildConfig.APPLICATION_ID + ".BROADCAST_REQUEST_TIMER_EXERCISE_STOP"
        const val BROADCAST_REQUEST_TIMER_REST_STOP = BuildConfig.APPLICATION_ID + ".BROADCAST_REQUEST_TIMER_REST_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        setupBroadcastReceivers()
    }

    override fun onDestroy() {
        mReceiver?.let { LocalBroadcastManager.getInstance(this).unregisterReceiver(it) }
        super.onDestroy()
    }

    override fun tag(): String {
        return "WorkoutService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when {
            intent?.action.equals(ACTION_SERVICE_START) -> handleStartService(intent)
            intent?.action.equals(ACTION_SERVICE_STOP) -> handleStopService()
            intent?.action.equals(ACTION_DECREASE_WEIGHT) -> handleWeightChange(false)
            intent?.action.equals(ACTION_INCREASE_WEIGHT) -> handleWeightChange(true)
            intent?.action.equals(ACTION_DECREASE_REPS) -> handleRepsChange(false)
            intent?.action.equals(ACTION_INCREASE_REPS) -> handleRepsChange(true)
            intent?.action.equals(ACTION_TIMER_REST_STOP) -> handleStopRestTimer()
            intent?.action.equals(ACTION_TIMER_EXERCISE_START) -> handleExerciseTimer(true)
            intent?.action.equals(ACTION_TIMER_EXERCISE_STOP) -> handleExerciseTimer(false)
            intent?.action.equals(ACTION_NEXT) -> handleComplete()
        }
        return START_STICKY
    }

    // Handlers

    private fun handleStartService(intent: Intent?) {
        if (NOTIFICATION_ID < 0) {
            NOTIFICATION_ID = Random().nextInt()
        }
        setupWorkoutState(intent)
        ELNotificationManager.startForeground(this,
                NOTIFICATION_ID,
                buildNotification(),
                notificationChannelOptions())
    }

    private fun handleStopService() {
        NOTIFICATION_ID = -1
        stopForeground(true)
        stopSelf()
    }

    private fun handleSetUpdated(intent: Intent?) {
        setupWorkoutState(intent)
        refreshNotification()
    }

    private fun handleShowRestTimer(intent: Intent?) {
        setupWorkoutState(intent)
        refreshNotification()
    }

    private fun handleWeightChange(increase: Boolean) {
        AnalyticsManager.manager.workoutServiceWeightModified()
        val intent = Intent(if (increase) BROADCAST_REQUEST_INCREASE_WEIGHT else BROADCAST_REQUEST_DECREASE_WEIGHT)
        intent.putExtra(ELConstants.EXTRA_WORKOUT_STATE, mNextState)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun handleRepsChange(increase: Boolean) {
        AnalyticsManager.manager.workoutServiceRepsModified()
        val intent = Intent(if (increase) BROADCAST_REQUEST_INCREASE_REPS else BROADCAST_REQUEST_DECREASE_REPS)
        intent.putExtra(ELConstants.EXTRA_WORKOUT_STATE, mNextState)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun handleExerciseTimer(start: Boolean) {
        if (start) {
            AnalyticsManager.manager.workoutServiceTimerStartedExercise()
        } else {
            AnalyticsManager.manager.workoutServiceTimerStoppedExercise()
        }
        val intent = Intent(if (start) BROADCAST_REQUEST_TIMER_EXERCISE_START else BROADCAST_REQUEST_TIMER_EXERCISE_STOP)
        intent.putExtra(ELConstants.EXTRA_WORKOUT_STATE, mNextState)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun handleComplete() {
        AnalyticsManager.manager.workoutServiceNextExercise()
        val intent = Intent(BROADCAST_REQUEST_COMPLETE_SET)
        intent.putExtra(ELConstants.EXTRA_WORKOUT_STATE, mNextState)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun handleStopRestTimer() {
        AnalyticsManager.manager.workoutServiceTimerStoppedRest()
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(BROADCAST_REQUEST_TIMER_REST_STOP))
    }

    private fun refreshNotification() {
        ELNotificationManager.notify(NOTIFICATION_ID,
                buildNotification(),
                notificationChannelOptions())
    }

    private fun buildNotification(): Notification {
        val state = WorkoutNotificationState.from(mWorkout, mRestTimer)
        return WorkoutNotificationBuilder(this).build(notificationChannelId(), state, mWorkout)
    }

    // Notification channel

    private fun notificationChannelId(): String {
        return getString(R.string.notification_channel_workout)
    }

    private fun notificationChannelOptions(): ELNotificationManager.NotificationChannelOptions {
        return ELNotificationManager.NotificationChannelOptions(notificationChannelId(),
                "Ongoing Workouts",
                "Keep track of your workout without unlocking your phone.",
                important = true,
                disableSound = true)
    }

    // Setup

    private fun setupBroadcastReceivers() {
        val filter = IntentFilter()
        filter.addAction(BROADCAST_SERVICE_SET_UPDATED)
        filter.addAction(BROADCAST_SERVICE_SHOW_REST_TIMER)
        mReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BROADCAST_SERVICE_SET_UPDATED -> handleSetUpdated(intent)
                    BROADCAST_SERVICE_SHOW_REST_TIMER -> handleShowRestTimer(intent)
                }
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver!!, filter)
    }

    private fun setupWorkoutState(intent: Intent?) {
        if (intent?.hasExtra(ELConstants.EXTRA_REST_TIMER_PROGRESS) == true) {
            // Rest timer updates don't carry the workout, so keep the last one we were sent.
            mRestTimer = WorkoutNotificationState.RestTimer(
                    remainingSeconds = intent.getIntExtra(ELConstants.EXTRA_REST_TIMER_REMAINING_SECONDS, 0),
                    remainingPercent = intent.getIntExtra(ELConstants.EXTRA_REST_TIMER_PROGRESS, 100))
        } else {
            mRestTimer = null
            mWorkout = intent?.getSerializableExtra(ELConstants.EXTRA_WORKOUT) as? ELWorkout
            mNextState = mWorkout?.getNextIncompleteState()
        }
    }
}
