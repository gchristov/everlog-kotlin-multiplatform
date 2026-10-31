package com.everlog.android.workout.data.notification

import com.everlog.android.common.data.model.workout.ELWorkoutState
import java.io.Serializable

/**
 * How the workout screens and WorkoutService talk, as local broadcasts within the app's process: the screen sends
 * the service the set it shows and the rest's countdown, and the service sends back the notification's buttons, each
 * with the set it showed.
 */
object WorkoutBroadcasts {
    private const val PREFIX = "com.everlog.workout."

    const val SERVICE_SET_UPDATED = PREFIX + "SERVICE_SET_UPDATED"
    const val SERVICE_SHOW_REST_TIMER = PREFIX + "SERVICE_SHOW_REST_TIMER"

    const val REQUEST_COMPLETE_SET = PREFIX + "REQUEST_NEXT_SET"
    const val REQUEST_INCREASE_WEIGHT = PREFIX + "REQUEST_INCREASE_WEIGHT"
    const val REQUEST_DECREASE_WEIGHT = PREFIX + "REQUEST_DECREASE_WEIGHT"
    const val REQUEST_INCREASE_REPS = PREFIX + "REQUEST_INCREASE_REPS"
    const val REQUEST_DECREASE_REPS = PREFIX + "REQUEST_DECREASE_REPS"
    const val REQUEST_INCREASE_TIME = PREFIX + "REQUEST_INCREASE_TIME"
    const val REQUEST_DECREASE_TIME = PREFIX + "REQUEST_DECREASE_TIME"
    const val REQUEST_TIMER_EXERCISE_STOP = PREFIX + "REQUEST_TIMER_EXERCISE_STOP"
    const val REQUEST_TIMER_REST_STOP = PREFIX + "REQUEST_TIMER_REST_STOP"

    const val EXTRA_WORKOUT_STATE = "EXTRA_WORKOUT_STATE"
    const val EXTRA_SHOWN_SET = "EXTRA_SHOWN_SET"
    const val EXTRA_REST_TIMER_PROGRESS = "EXTRA_REST_TIMER_PROGRESS"
    const val EXTRA_REST_TIMER_REMAINING_SECONDS = "EXTRA_REST_TIMER_REMAINING_SECONDS"
}

/**
 * The set the workout screen has selected, which the notification shows, and whether its main button starts the
 * set's timer rather than completing it.
 */
data class ShownSet(val position: ELWorkoutState, val startsTimer: Boolean) : Serializable
