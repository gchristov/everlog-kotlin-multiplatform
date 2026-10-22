package com.everlog.ui.activities.setsuggestions

import android.content.Context
import androidx.annotation.StringRes
import com.everlog.R
import com.everlog.data.controllers.statistics.BaseStatsController
import com.everlog.data.controllers.workoutprefill.goalWeight
import com.everlog.data.controllers.workoutprefill.rounded1RM
import com.everlog.managers.preferences.SettingsManager.MuscleGoal
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import kotlin.math.roundToInt

/**
 * The options on the screen, each stored as the muscle goal the old screen sets, so either screen and older app
 * versions read the same setting.
 */
enum class SetSuggestion(val goal: MuscleGoal, @StringRes val title: Int) {
    Off(MuscleGoal.NONE, R.string.set_suggestions_off),
    LastTime(MuscleGoal.HISTORY, R.string.set_suggestions_last_time),
    BuildMuscle(MuscleGoal.GROWTH, R.string.set_suggestions_build_muscle),
    GetStronger(MuscleGoal.POWER, R.string.set_suggestions_get_stronger),
    SpeedAndPower(MuscleGoal.EXPLOSIVENESS, R.string.set_suggestions_speed_power);

    val forGoal: Boolean get() = goal.is1RMBased()

    val percent: Int get() = (goal.percent1RM() * 100).roundToInt()

    fun subtitle(context: Context): String = when (this) {
        Off -> context.getString(R.string.set_suggestions_off_subtitle)
        LastTime -> context.getString(R.string.set_suggestions_last_time_subtitle)
        else -> context.getString(R.string.set_suggestions_goal_subtitle, percent)
    }

    companion object {
        val simple = values().filterNot { it.forGoal }
        val goals = values().filter { it.forGoal }

        // Null for goals the app no longer offers (endurance, strength), which older versions could set
        fun of(goal: MuscleGoal): SetSuggestion? = values().firstOrNull { it.goal == goal }
    }
}

data class SetSuggestionsState(
    val selected: SetSuggestion?,
    val unit: WeightUnit,
    val showingHow: Boolean = false,
) {
    val example: GoalWeightExample get() = GoalWeightExample.of(unit)
}

/**
 * The fixed example on the how sheet, a lift almost everyone knows, worked out the same way as the suggestions it
 * explains. The set is a round number in each unit.
 */
data class GoalWeightExample(
    val weight: Float,
    val reps: Int,
    val oneRepMax: Float,
    val goal: SetSuggestion,
    val goalWeight: Float,
) {
    companion object {
        private const val Reps = 5

        fun of(unit: WeightUnit): GoalWeightExample {
            val weight = if (unit == WeightUnit.POUND) 180f else 80f
            val oneRepMax = BaseStatsController.estimate1RM(weight, Reps)
            return GoalWeightExample(
                weight = weight,
                reps = Reps,
                oneRepMax = rounded1RM(oneRepMax),
                goal = SetSuggestion.BuildMuscle,
                goalWeight = goalWeight(oneRepMax, SetSuggestion.BuildMuscle.goal),
            )
        }
    }
}
