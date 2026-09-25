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
import com.everlog.constants.ELConstants
import com.everlog.data.model.set.ELSetType
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.preferences.SettingsManager
import com.everlog.services.workout.WorkoutNotificationState.Done
import com.everlog.services.workout.WorkoutNotificationState.NextSet
import com.everlog.services.workout.WorkoutNotificationState.NoExercises
import com.everlog.services.workout.WorkoutNotificationState.Rest
import com.everlog.ui.activities.home.workout.WorkoutActivity
import com.everlog.utils.ArrayResourceTypeUtils
import com.everlog.utils.format.FormatUtils
import java.util.concurrent.TimeUnit

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
                WorkoutService.ACTION_TIMER_EXERCISE_START,
                WorkoutService.ACTION_TIMER_EXERCISE_STOP,
                WorkoutService.ACTION_TIMER_REST_STOP,
                WorkoutService.ACTION_NEXT)
    }

    fun build(channelId: String, state: WorkoutNotificationState, workout: ELWorkout?): Notification {
        val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.main_accent))
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
    }

    private fun buildNextSetCollapsed(state: NextSet): RemoteViews {
        val layout = RemoteViews(context.packageName, R.layout.notification_workout_set)
        layout.setTextViewText(R.id.exerciseName, exerciseName(state))
        layout.setTextViewText(R.id.setSummary, setSummary(state))
        layout.setOnClickPendingIntent(R.id.nextBtn, actionIntent(WorkoutService.ACTION_NEXT))
        return layout
    }

    private fun buildNextSetExpanded(state: NextSet): RemoteViews {
        val layout = RemoteViews(context.packageName, R.layout.notification_workout_set_big)
        layout.setTextViewText(R.id.exerciseName, exerciseName(state))
        layout.setTextViewText(R.id.setSummary, setLabel(state))
        // Weight
        layout.setTextViewText(R.id.weightField, FormatUtils.formatSetWeight(state.weight))
        layout.setTextViewText(R.id.weightUnit, SettingsManager.weightUnitAbbreviation())
        layout.setOnClickPendingIntent(R.id.reduceWeightBtn, actionIntent(WorkoutService.ACTION_DECREASE_WEIGHT))
        layout.setOnClickPendingIntent(R.id.increaseWeightBtn, actionIntent(WorkoutService.ACTION_INCREASE_WEIGHT))
        // Reps
        layout.setViewVisibility(R.id.repsPanel, if (state.reps != null) View.VISIBLE else View.GONE)
        layout.setTextViewText(R.id.repsField, (state.reps ?: 0).toString())
        layout.setOnClickPendingIntent(R.id.reduceRepsBtn, actionIntent(WorkoutService.ACTION_DECREASE_REPS))
        layout.setOnClickPendingIntent(R.id.increaseRepsBtn, actionIntent(WorkoutService.ACTION_INCREASE_REPS))
        // Time
        layout.setViewVisibility(R.id.timePanel, if (state.timeSeconds != null) View.VISIBLE else View.GONE)
        layout.setTextViewText(R.id.timeField, formatTime(state.timeSeconds ?: 0))
        layout.setViewVisibility(R.id.startTimerBtn, if (state.timerRunning) View.GONE else View.VISIBLE)
        layout.setViewVisibility(R.id.stopTimerBtn, if (state.timerRunning) View.VISIBLE else View.GONE)
        layout.setOnClickPendingIntent(R.id.startTimerBtn, actionIntent(WorkoutService.ACTION_TIMER_EXERCISE_START))
        layout.setOnClickPendingIntent(R.id.stopTimerBtn, actionIntent(WorkoutService.ACTION_TIMER_EXERCISE_STOP))
        // Next
        layout.setOnClickPendingIntent(R.id.nextBtn, actionIntent(WorkoutService.ACTION_NEXT))
        return layout
    }

    // Rest

    private fun renderRest(builder: NotificationCompat.Builder, state: Rest) {
        val title = if (state.remainingSeconds > 0) {
            context.getString(R.string.workout_rest_time, formatTime(state.remainingSeconds))
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
                .addAction(R.drawable.ic_clear_white,
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
     * E.g. "Set 2/3 • 8 x 60 kg" or "Set 1/3 • 40 sec • 20 kg". The values use the same wording as
     * a completed set's row in the workout screen (ELSet.getExerciseSetSummary). A set with nothing
     * entered yet gets a prompt to open the workout and fill it in, e.g. "Set 1/1 • Tap to edit".
     */
    private fun setSummary(state: NextSet, promptIfEmpty: Boolean = true): String {
        // Show the countdown while the exercise timer runs
        val set = if (state.timerRunning && state.timeSeconds != null) {
            state.set.copy(timeSeconds = state.timeSeconds)
        } else {
            state.set
        }
        val values = set.getExerciseSetSummary(context, true)
                ?: if (promptIfEmpty) context.getString(R.string.workout_notification_tap_to_edit) else null
        return listOfNotNull(setLabel(state), values).joinToString(" • ")
    }

    private fun formatTime(seconds: Int): String {
        return FormatUtils.formatDurationShort(TimeUnit.SECONDS.toMillis(seconds.toLong()), "mm:ss")
    }

    // Intents

    private fun openWorkoutIntent(workout: ELWorkout?): PendingIntent {
        val intent = Intent(context, WorkoutActivity::class.java)
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
