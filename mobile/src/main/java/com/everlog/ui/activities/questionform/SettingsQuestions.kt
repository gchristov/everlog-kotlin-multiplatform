package com.everlog.ui.activities.questionform

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.auth.LocalUserManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.utils.ArrayResourceTypeUtils
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.TextStyle
import java.util.Locale

// The app's settings as questions, starting from their current values. Copy is hardcoded while this
// is a prototype.
internal object SettingsQuestions {
    private const val WeightUnit = "weightUnit"
    private const val WeeklyGoal = "weeklyGoal"
    private const val MuscleGoal = "muscleGoal"
    private const val WeightIncrease = "weightIncrease"
    private const val FirstDayOfWeek = "firstDayOfWeek"

    private val FirstDayOfWeekOptions = listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)

    fun build(context: Context): List<Question> {
        val settings = SettingsManager.manager
        val isPro = LocalUserManager.getUser()?.isPro() == true
        return listOf(
            Question.SingleChoice(
                id = WeightUnit,
                title = "Which weight unit do you use?",
                options = listOf(
                    Question.Option(SettingsManager.WeightUnit.KILOGRAM.name, context.getString(R.string.settings_kilograms)),
                    Question.Option(SettingsManager.WeightUnit.POUND.name, context.getString(R.string.settings_pounds)),
                ),
                default = Answer.SingleChoice(settings.weightUnit().name),
            ),
            Question.Number(
                id = WeeklyGoal,
                title = "How many workouts a week are you aiming for?",
                // Same range as the picker in Settings
                min = 1.0,
                max = 20.0,
                default = Answer.Number(settings.weeklyWorkoutsGoal().toDouble()),
                unit = { "workouts" },
            ),
            Question.SingleChoice(
                id = MuscleGoal,
                title = "What's your muscle training goal?",
                options = SettingsManager.MuscleGoal.availableGoals().map { goal ->
                    Question.Option(
                        id = goal.name,
                        label = goal.valueName(context) + if (goal.proLocked() && !isPro) " (Pro)" else "",
                        description = goal.valueSettingsSummary(context, false),
                        // Pro goals need an upgrade first, which this form doesn't offer
                        enabled = !goal.proLocked() || isPro,
                    )
                },
                default = Answer.SingleChoice(settings.muscleGoal().name),
            ),
            Question.Number(
                id = WeightIncrease,
                title = "How much should weights go up by with +/-?",
                // Same range as the picker in Settings: 1 to 20.75 in quarters
                min = 1.0,
                max = 20.75,
                step = 0.25,
                default = Answer.Number(settings.weightIncrease().toDouble()),
                unit = { answers ->
                    val unit = (answers[WeightUnit] as? Answer.SingleChoice)?.optionId ?: settings.weightUnit().name
                    ArrayResourceTypeUtils.withWeightAbbreviations().getTitle(unit, "--")
                },
            ),
            Question.SingleChoice(
                id = FirstDayOfWeek,
                title = "Which day does your week start on?",
                options = FirstDayOfWeekOptions.map { day ->
                    Question.Option(day.name, day.getDisplayName(TextStyle.FULL, Locale.getDefault()))
                },
                default = Answer.SingleChoice(settings.firstDayOfWeek().name),
            ),
        )
    }

    // Saves the answers that changed a setting, logging the same analytics as Settings
    fun save(context: Context, answers: Map<String, Answer>) {
        val settings = SettingsManager.manager
        val analytics = AnalyticsManager.manager
        var changed = false

        answers.choice(WeightUnit)?.let { SettingsManager.WeightUnit.valueOf(it) }
            ?.takeIf { it != settings.weightUnit() }
            ?.let { unit ->
                settings.setWeightUnit(unit)
                analytics.settingsWeightUnitModified(unit.name)
                changed = true
            }
        answers.number(WeeklyGoal)?.toInt()
            ?.takeIf { it != settings.weeklyWorkoutsGoal() }
            ?.let { goal ->
                settings.setWeeklyWorkoutsGoal(goal)
                analytics.settingsWeeklyGoalModified(goal)
                changed = true
            }
        answers.choice(MuscleGoal)?.let { SettingsManager.MuscleGoal.valueOf(it) }
            ?.takeIf { it != settings.muscleGoal() }
            ?.let { goal ->
                settings.setMuscleGoal(goal)
                analytics.settingsMuscleGoalModified(goal)
                changed = true
            }
        answers.number(WeightIncrease)?.toFloat()
            ?.takeIf { it != settings.weightIncrease() }
            ?.let { increase ->
                settings.setWeightIncrease(increase)
                analytics.settingsWeightModified(increase)
                changed = true
            }
        answers.choice(FirstDayOfWeek)?.let { DayOfWeek.valueOf(it) }
            ?.takeIf { it != settings.firstDayOfWeek() }
            ?.let { day ->
                settings.setFirstDayOfWeek(day)
                analytics.settingsFirstWeekDayModified(day.name)
                changed = true
            }

        // Lets open screens (e.g. the week view) pick up the new settings
        if (changed) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(ELConstants.BROADCAST_PREFERENCES_CHANGED))
        }
    }

    private fun Map<String, Answer>.choice(id: String) = (get(id) as? Answer.SingleChoice)?.optionId

    private fun Map<String, Answer>.number(id: String) = (get(id) as? Answer.Number)?.value
}
