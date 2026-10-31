package com.everlog.services.workout

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.settings.data.SettingsManager
import com.everlog.constants.ELConstants
import com.everlog.android.common.session.toSessionSet
import com.everlog.services.workout.WorkoutNotificationState.Done
import com.everlog.services.workout.WorkoutNotificationState.NextSet
import com.everlog.services.workout.WorkoutNotificationState.NoExercises
import com.everlog.services.workout.WorkoutNotificationState.Rest
import com.everlog.android.workout.feature.log.WorkoutLogActivity
import com.everlog.android.common.kotlin.format.ArrayResourceTypeUtils
import com.everlog.android.common.kotlin.format.EmptyValue
import com.everlog.android.common.kotlin.format.displayWeight
import com.everlog.android.common.kotlin.format.formatClock
import com.everlog.android.common.kotlin.format.formatWeight
import com.everlog.android.common.sessionui.exercisetable.lastTimeText
import com.everlog.android.common.kotlin.format.unitLabel
import com.everlog.android.common.design.R as DesignR

/**
 * Renders a [WorkoutNotificationState] as the ongoing workout notification.
 *
 * The system owns the notification chrome (icon, app name, colours, expand affordance) on every
 * Android version. Only the next set view uses a custom layout, and it's wrapped in
 * [NotificationCompat.DecoratedCustomViewStyle] so it's decorated the same way from API 23
 * (backported by NotificationCompat) through to API 31+ (where the system always decorates
 * custom views). The other states use the standard template.
 */
class WorkoutNotificationBuilder(private val context: Context) {

    companion object {
        // Stable request codes so each update replaces the previous PendingIntents rather than
        // creating new ones.
        private const val REQUEST_OPEN_WORKOUT = 0
        private val ACTION_REQUEST_CODES = listOf(
                WorkoutService.ACTION_DECREASE_WEIGHT,
                WorkoutService.ACTION_INCREASE_WEIGHT,
                WorkoutService.ACTION_DECREASE_REPS,
                WorkoutService.ACTION_INCREASE_REPS,
                WorkoutService.ACTION_TIMER_EXERCISE_STOP,
                WorkoutService.ACTION_TIMER_REST_STOP,
                WorkoutService.ACTION_NEXT,
                WorkoutService.ACTION_DECREASE_TIME,
                WorkoutService.ACTION_INCREASE_TIME)
    }

    fun build(channelId: String, state: WorkoutNotificationState, workout: ELWorkout?): Notification {
        val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, DesignR.color.main_accent))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                // API 31+ can otherwise delay showing a foreground service notification by up to 10s
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(openWorkoutIntent(workout))
        when (state) {
            is NextSet -> renderNextSet(builder, state)
            is Rest -> renderRest(builder, state)
            is Done -> builder
                    .setContentTitle(context.getString(R.string.workout_empty_done))
                    .setContentText(context.getString(R.string.workout_empty_done_subtitle))
            is NoExercises -> builder
                    .setContentTitle(context.getString(R.string.workout_empty_no_sets))
                    .setContentText(context.getString(R.string.workout_empty_no_sets_subtitle))
        }
        return builder.build()
    }

    // Next set

    private fun renderNextSet(builder: NotificationCompat.Builder, state: NextSet) {
        // Title and text aren't shown alongside the custom views, but are still used for
        // accessibility and by surfaces that don't render custom views (e.g. wearables).
        builder
                .setContentTitle(exerciseName(state))
                .setContentText(setSummary(state))
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(buildNextSetCollapsed(state))
                .setCustomBigContentView(buildNextSetExpanded(state))
                // The heads-up shows the action under it, so it leaves out its own main button
                .setCustomHeadsUpContentView(buildNextSetCollapsed(state, withMainButton = false))
                .addAction(0, mainAction(state), actionIntent(WorkoutService.ACTION_NEXT))
        if (state.timerRunning) {
            builder.addAction(0, context.getString(R.string.workout_notification_stop_timer), actionIntent(WorkoutService.ACTION_TIMER_EXERCISE_STOP))
        }
    }

    // The same as the workout screen's main button
    private fun mainAction(state: NextSet): String = context.getString(
            if (state.startsTimer) R.string.workout_notification_start_timer else R.string.workout_notification_complete_set)

    private fun buildNextSetCollapsed(state: NextSet, withMainButton: Boolean = true): RemoteViews {
        val layout = RemoteViews(context.packageName, R.layout.notification_workout_set)
        layout.setTextViewText(R.id.exerciseName, exerciseName(state))
        layout.setTextViewText(R.id.setSummary, setSummary(state))
        layout.setViewVisibility(R.id.nextBtn, if (withMainButton) View.VISIBLE else View.GONE)
        layout.setTextViewText(R.id.nextBtn, mainAction(state))
        layout.setOnClickPendingIntent(R.id.nextBtn, actionIntent(WorkoutService.ACTION_NEXT))
        return layout
    }

    private fun buildNextSetExpanded(state: NextSet): RemoteViews {
        val layout = RemoteViews(context.packageName, R.layout.notification_workout_set_big)
        layout.setTextViewText(R.id.exerciseName, exerciseName(state))
        layout.setTextViewText(R.id.setSummary, setLabel(state))
        // Weight
        layout.setTextViewText(R.id.weightField, state.weightKg?.let { formatWeight(displayWeight(it, SettingsManager.manager.weightUnit())) } ?: EmptyValue)
        layout.setTextViewText(R.id.weightUnit, unitLabel(SettingsManager.manager.weightUnit()))
        layout.setOnClickPendingIntent(R.id.reduceWeightBtn, actionIntent(WorkoutService.ACTION_DECREASE_WEIGHT))
        layout.setOnClickPendingIntent(R.id.increaseWeightBtn, actionIntent(WorkoutService.ACTION_INCREASE_WEIGHT))
        // Reps
        layout.setViewVisibility(R.id.repsPanel, if (state.timed) View.GONE else View.VISIBLE)
        layout.setTextViewText(R.id.repsField, state.reps?.toString() ?: EmptyValue)
        layout.setOnClickPendingIntent(R.id.reduceRepsBtn, actionIntent(WorkoutService.ACTION_DECREASE_REPS))
        layout.setOnClickPendingIntent(R.id.increaseRepsBtn, actionIntent(WorkoutService.ACTION_INCREASE_REPS))
        // Time
        layout.setViewVisibility(R.id.timePanel, if (state.timed) View.VISIBLE else View.GONE)
        layout.setTextViewText(R.id.timeField, state.timeSeconds?.let { formatClock(it.toLong()) } ?: EmptyValue)
        layout.setOnClickPendingIntent(R.id.reduceTimeBtn, actionIntent(WorkoutService.ACTION_DECREASE_TIME))
        layout.setOnClickPendingIntent(R.id.increaseTimeBtn, actionIntent(WorkoutService.ACTION_INCREASE_TIME))
        return layout
    }

    // Rest

    private fun renderRest(builder: NotificationCompat.Builder, state: Rest) {
        val title = if (state.remainingSeconds > 0) {
            context.getString(R.string.workout_rest_time, formatClock(state.remainingSeconds.toLong()))
        } else {
            context.getString(R.string.workout_notification_rest)
        }
        val upNext = upNext(state)
        builder
                .setContentTitle(title)
                .setContentText(upNext)
                // The expanded standard template drops the text in favour of the progress bar on
                // API 31+. The big text template shows both.
                .setStyle(NotificationCompat.BigTextStyle().bigText(upNext))
                .setProgress(100, state.remainingPercent, false)
                .addAction(DesignR.drawable.ic_clear_white,
                        context.getString(R.string.workout_notification_skip_rest),
                        actionIntent(WorkoutService.ACTION_TIMER_REST_STOP))
    }

    private fun upNext(state: Rest): String {
        val next = state.upNext ?: return context.getString(R.string.workout_notification_complete)
        return context.getString(R.string.workout_notification_up_next, exerciseName(next), setSummary(next, promptIfEmpty = false))
    }

    // Text

    private fun exerciseName(state: NextSet): String {
        return if (state.exercisesInGroup > 1) {
            "${state.exercisePosition}/${state.exercisesInGroup} ${state.exerciseName}"
        } else {
            state.exerciseName
        }
    }

    /**
     * E.g. "Set 1/1", "Set 2/3" or "Super Set 3/4", naming sets the same way as the workout screen.
     */
    private fun setLabel(state: NextSet): String {
        val type = if (state.setType == ELSetType.SINGLE.name) {
            context.getString(R.string.workout_notification_set)
        } else {
            ArrayResourceTypeUtils.withSetTypes().getTitle(state.setType, state.setType.lowercase().replaceFirstChar { it.uppercase() } + " Set") ?: ""
        }
        return "$type ${state.setNumber}/${state.totalSets}"
    }

    /**
     * E.g. "Set 2/3 • 8 × 60 kg" or "Set 1/3 • 0:40 × 20 kg", as the workout table's last time reads, with a
     * running timer's seconds left. A set with nothing entered yet gets a prompt to open the workout and fill it in,
     * e.g. "Set 1/1 • Tap to edit".
     */
    private fun setSummary(state: NextSet, promptIfEmpty: Boolean = true): String {
        val unit = SettingsManager.manager.weightUnit()
        val set = state.set.toSessionSet().copy(timeSeconds = state.timeSeconds)
        val values = lastTimeText(set, unit, unitLabel(unit))
                ?: if (promptIfEmpty) context.getString(R.string.workout_notification_tap_to_edit) else null
        return listOfNotNull(setLabel(state), values).joinToString(" • ")
    }

    // Intents

    private fun openWorkoutIntent(workout: ELWorkout?): PendingIntent {
        val intent = Intent(context, WorkoutLogActivity::class.java)
        intent.putExtra(ELConstants.EXTRA_WORKOUT, workout)
        intent.action = WorkoutService.ACTION_OPEN_WORKOUT
        return PendingIntent.getActivity(context, REQUEST_OPEN_WORKOUT, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun actionIntent(action: String): PendingIntent {
        val intent = Intent(context, WorkoutService::class.java)
        intent.action = action
        // Offset from REQUEST_OPEN_WORKOUT
        val requestCode = ACTION_REQUEST_CODES.indexOf(action) + 1
        return PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
