package com.everlog.services.workout

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.activities.home.workout.WorkoutActivity
import com.everlog.utils.ArrayResourceTypeUtils
import com.everlog.utils.device.DeviceUtils
import com.everlog.utils.format.FormatUtils
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.random.Random

class WorkoutNotificationBuilder(service: WorkoutService) {

    private var mService: WorkoutService = service

    internal fun buildNotification(channelId: String): Notification {
        val builder = NotificationCompat.Builder(mService, channelId)
        builder
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_notification)
        if (mService.mShowRestTimer) {
            // Rest timer
            val layout = buildNotificationRestTimerLayout()
            builder
                    .setCustomContentView(layout)
                    .setCustomBigContentView(layout)
        } else if (mService.mNextState == null || mService.mWorkout?.hasExercises() == false) {
            // Empty workout or reached end
            val layout = buildNotificationWorkoutEmptyLayout()
            builder
                    .setCustomContentView(layout)
                    .setCustomBigContentView(layout)
        } else {
            // Next set to complete
            builder
                    .setCustomContentView(buildNotificationWorkoutLayout(R.layout.notification_workout))
                    .setCustomBigContentView(buildNotificationWorkoutLayout(R.layout.notification_workout_big))
        }
        if (!DeviceUtils.isAndroidO()) {
            builder.priority = Notification.PRIORITY_HIGH
        }
        val notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_FOREGROUND_SERVICE
        return notification
    }

    private fun buildNotificationWorkoutEmptyLayout(): RemoteViews {
        val layout = RemoteViews(packageName(), R.layout.notification_workout_empty)
        // Clicks
        layout.setOnClickPendingIntent(R.id.notification_background, buildOpenAppPendingIntent())
        // State
        if (mService.mWorkout?.hasExercises() == true) {
            layout.setTextViewText(R.id.titleLbl, mService.getString(R.string.workout_empty_done))
            layout.setTextViewText(R.id.subtitleLbl, mService.getString(R.string.workout_empty_done_subtitle))
        } else {
            layout.setTextViewText(R.id.titleLbl, mService.getString(R.string.workout_empty_no_sets))
            layout.setTextViewText(R.id.subtitleLbl, mService.getString(R.string.workout_empty_no_sets_subtitle))
        }
        return layout
    }

    private fun buildNotificationRestTimerLayout(): RemoteViews {
        val layout = RemoteViews(packageName(), R.layout.notification_workout_rest_timer)
        // Clicks
        layout.setOnClickPendingIntent(R.id.notification_background, buildOpenAppPendingIntent())
        layout.setOnClickPendingIntent(R.id.stopTimerBtn, buildActionPendingIntent(WorkoutService.ACTION_TIMER_REST_STOP))
        // State
        if (mService.mShowRestTimerProgress > 0) {
            renderRestTime(layout)
        } else {
            layout.setTextViewText(R.id.restTimeRemainingLbl, "Rest")
        }
        return layout
    }

    private fun buildNotificationWorkoutLayout(layoutResId: Int): RemoteViews {
        val layout = RemoteViews(packageName(), layoutResId)
        // Clicks
        layout.setOnClickPendingIntent(R.id.notification_background, buildOpenAppPendingIntent())
        layout.setOnClickPendingIntent(R.id.reduceWeightBtn, buildActionPendingIntent(WorkoutService.ACTION_DECREASE_WEIGHT))
        layout.setOnClickPendingIntent(R.id.increaseWeightBtn, buildActionPendingIntent(WorkoutService.ACTION_INCREASE_WEIGHT))
        layout.setOnClickPendingIntent(R.id.reduceRepsBtn, buildActionPendingIntent(WorkoutService.ACTION_DECREASE_REPS))
        layout.setOnClickPendingIntent(R.id.increaseRepsBtn, buildActionPendingIntent(WorkoutService.ACTION_INCREASE_REPS))
        layout.setOnClickPendingIntent(R.id.startTimerBtn, buildActionPendingIntent(WorkoutService.ACTION_TIMER_EXERCISE_START))
        layout.setOnClickPendingIntent(R.id.stopTimerBtn, buildActionPendingIntent(WorkoutService.ACTION_TIMER_EXERCISE_STOP))
        layout.setOnClickPendingIntent(R.id.nextBtn, buildActionPendingIntent(WorkoutService.ACTION_NEXT))
        // State
        if (mService.mWorkout != null) {
            renderExerciseNameAndSet(layout)
            renderWeight(layout)
            renderReps(layout)
            renderTime(layout)
        } else {
            layout.setTextViewText(R.id.exerciseName, "--")
        }
        return layout
    }

    private fun buildOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(mService, WorkoutActivity::class.java)
        intent.putExtra(ELConstants.EXTRA_WORKOUT, mService.mWorkout)
        intent.action = WorkoutService.ACTION_OPEN_WORKOUT
        return PendingIntent.getActivity(mService, Random.nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    private fun buildActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(mService, WorkoutService::class.java)
        intent.action = action
        return PendingIntent.getService(mService, Random.nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    // Render

    private fun renderRestTime(layout: RemoteViews) {
        layout.setProgressBar(R.id.setControlOuterProgress, 100, mService.mShowRestTimerProgress, false)
        layout.setTextViewText(R.id.restTimeRemainingLbl, mService.getString(R.string.workout_rest_time, FormatUtils.formatDurationShort((mService.mShowRestTimerRemainingSeconds * 1000).toLong(), "mm:ss")))
        layout.setTextViewText(R.id.pendingExerciseField, getPendingActivity())
    }

    private fun getPendingActivity(): String {
        if (mService.mNextState == null) {
            return "Workout complete"
        }
        val exerciseName = mService.mNextExercise?.getNotificationName(mService)
        val type = mService.mNextExerciseGroup?.type
        val setDescription = mService.mNextSet?.getOngoingWorkoutNotificationSummary(mService, (mService.mNextState?.setIndex ?: 0) + 1, type!!)
        return String.format("%s  •  %s", exerciseName, setDescription)
    }

    private fun renderExerciseNameAndSet(layout: RemoteViews) {
        // Calculations
        var setType = ArrayResourceTypeUtils.withSetTypes().getTitle(mService.mNextExerciseGroup?.type!!, mService.mNextExerciseGroup?.type?.lowercase()?.capitalize() + " Set")
        val completedSets = (mService.mNextState?.setIndex ?: 0) + 1
        val totalSets = mService.mNextExerciseGroup?.getTotalSetsCount() ?: 0
        val exerciseName = mService.mNextExercise?.getName()
        val exercisesInGroup = mService.mNextState?.exercisesInGroup ?: 1
        val exerciseIndex = mService.mNextState?.exerciseIndex ?: 0
        // Set labels
        if (exercisesInGroup > 1) {
            layout.setTextViewText(R.id.exerciseName, String.format("%d/%d %s", exerciseIndex + 1, exercisesInGroup, exerciseName))
        } else {
            layout.setTextViewText(R.id.exerciseName, String.format("%s", exerciseName))
        }
        if (totalSets > 1) {
            setType += String.format(" %d/%d", completedSets, totalSets)
        }
        layout.setTextViewText(R.id.setSummary, setType)
    }

    private fun renderWeight(layout: RemoteViews) {
        val set = mService.mNextSet
        layout.setTextViewText(R.id.weightField, if (set?.isWeightEntered() == false) 0.toString() else FormatUtils.formatSetWeight(set?.getWeight() ?: 0f))
        layout.setTextViewText(R.id.weightUnit, SettingsManager.weightUnitAbbreviation())
    }

    private fun renderReps(layout: RemoteViews) {
        val set = mService.mNextSet
        layout.setViewVisibility(R.id.repsPanel, if (set?.canShowRepOptions() == true) View.VISIBLE else View.GONE)
        layout.setTextViewText(R.id.repsField, max(0, set?.getReps() ?: 0).toString())
    }

    private fun renderTime(layout: RemoteViews) {
        val set = mService.mNextSet
        layout.setViewVisibility(R.id.timePanel, if (set?.canShowTimeOptions() == true) View.VISIBLE else View.GONE)
        layout.setViewVisibility(R.id.startTimerBtn, if (set?.remainingTimeSeconds != null) View.GONE else View.VISIBLE)
        layout.setViewVisibility(R.id.stopTimerBtn, if (set?.remainingTimeSeconds == null) View.GONE else View.VISIBLE)
        layout.setTextViewText(R.id.timeField, FormatUtils.formatDurationShort(TimeUnit.SECONDS.toMillis(if (set?.remainingTimeSeconds != null) set.remainingTimeSeconds?.toLong() ?: 0 else set?.getTimeSeconds()?.toLong() ?: 0), "mm:ss"))
    }

    private fun packageName(): String {
        return mService.packageName
    }
}
