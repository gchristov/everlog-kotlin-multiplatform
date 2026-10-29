package com.everlog.managers.apprate

import com.everlog.config.AppConfig
import com.everlog.data.model.workout.ELWorkout
import java.util.concurrent.TimeUnit

/**
 * Something the user just did that might be a good moment to ask for a rating, passed to
 * [com.everlog.ui.navigator.Navigator.requestRatePrompt].
 *
 * Each trigger takes the data its rule needs and decides whether the action qualifies. Qualifying
 * actions from every trigger count towards the same goal in [RatePromptPacing], which decides when
 * to actually prompt.
 */
sealed class RatePromptTrigger {

    /**
     * Name of the trigger in analytics.
     */
    abstract val source: String

    /**
     * Identifies this particular action, so it only counts once if it's reported again, e.g. when the
     * screen is recreated.
     */
    abstract val actionId: String

    abstract fun isEligible(): Boolean

    /**
     * A workout was just finished. Short ones don't count, as they're likely tests or accidental saves.
     */
    class WorkoutCompleted(private val workout: ELWorkout) : RatePromptTrigger() {

        override val source = "workout_completed"

        override val actionId = "$source:${workout.uuid}"

        override fun isEligible(): Boolean {
            return workout.getDurationMillis() >= TimeUnit.MINUTES.toMillis(AppConfig.configuration.ratePromptMinWorkoutMinutes.toLong())
        }
    }
}
