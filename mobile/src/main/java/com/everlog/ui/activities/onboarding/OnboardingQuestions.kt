package com.everlog.ui.activities.onboarding

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.everlog.R
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.TextStyle
import java.util.Locale

// The setup questions from the Everlog Onboarding design's copy deck. The copy is in strings.xml.

sealed interface OnboardingQuestion {
    val id: String
    // Shown on the collapsed summary row
    @get:StringRes val label: Int
    @get:StringRes val title: Int
    @get:StringRes val helper: Int?

    data class Choice(
        override val id: String,
        @StringRes override val label: Int,
        @StringRes override val title: Int,
        @StringRes override val helper: Int? = null,
        val options: List<Option>,
        // Two large cards side by side (units) instead of a list of answer cards
        val sideBySide: Boolean = false,
    ) : OnboardingQuestion

    data class Days(
        override val id: String,
        @StringRes override val label: Int,
        @StringRes override val title: Int,
        @StringRes override val helper: Int?,
        val options: List<Int>,
        // How a number of days reads on the summary row
        @PluralsRes val answer: Int,
    ) : OnboardingQuestion

    data class Reminders(
        override val id: String,
        @StringRes override val label: Int,
        @StringRes override val title: Int,
        @StringRes override val helper: Int?,
    ) : OnboardingQuestion

    data class Option(
        val id: String,
        @StringRes val title: Int,
        @StringRes val description: Int? = null,
        // How the answer reads on the summary row
        @StringRes val summary: Int = title,
    )
}

sealed interface Answer {
    data class Choice(val optionId: String) : Answer
    data class Days(val count: Int) : Answer
    data class Reminders(val days: Set<DayOfWeek>, val hour: Int, val minute: Int) : Answer
    data object RemindersOff : Answer
}

internal object OnboardingQuestions {
    const val Units = "units"
    const val Days = "days"
    const val Where = "where"
    const val Experience = "experience"
    const val Goal = "goal"
    const val Reminders = "reminders"

    const val Kilograms = "kg"
    const val Pounds = "lb"
    const val Gym = "gym"
    const val Home = "home"
    const val Bodyweight = "bodyweight"
    const val JustStarting = "justStarting"
    const val AWhile = "aWhile"
    const val Years = "years"
    const val GetStronger = "stronger"
    const val BuildMuscle = "muscle"
    const val BuildEndurance = "endurance"
    const val StayFit = "fit"

    val all = listOf(
        OnboardingQuestion.Choice(
            id = Units,
            label = R.string.onboarding_units_label,
            title = R.string.onboarding_units_title,
            helper = R.string.onboarding_units_helper,
            options = listOf(
                OnboardingQuestion.Option(Kilograms, R.string.kg, R.string.onboarding_units_kilograms, summary = R.string.onboarding_units_kilograms),
                OnboardingQuestion.Option(Pounds, R.string.lb, R.string.onboarding_units_pounds, summary = R.string.onboarding_units_pounds),
            ),
            sideBySide = true,
        ),
        OnboardingQuestion.Days(
            id = Days,
            label = R.string.onboarding_days_label,
            title = R.string.onboarding_days_title,
            helper = R.string.onboarding_days_helper,
            options = (1..7).toList(),
            answer = R.plurals.onboarding_days_answer,
        ),
        OnboardingQuestion.Choice(
            id = Where,
            label = R.string.onboarding_where_label,
            title = R.string.onboarding_where_title,
            options = listOf(
                OnboardingQuestion.Option(Gym, R.string.onboarding_where_gym, R.string.onboarding_where_gym_description),
                OnboardingQuestion.Option(Home, R.string.onboarding_where_home, R.string.onboarding_where_home_description, summary = R.string.onboarding_where_home_answer),
                OnboardingQuestion.Option(Bodyweight, R.string.onboarding_where_bodyweight, R.string.onboarding_where_bodyweight_description, summary = R.string.onboarding_where_bodyweight_answer),
            ),
        ),
        OnboardingQuestion.Choice(
            id = Experience,
            label = R.string.onboarding_experience_label,
            title = R.string.onboarding_experience_title,
            options = listOf(
                OnboardingQuestion.Option(JustStarting, R.string.onboarding_experience_just_starting, R.string.onboarding_experience_just_starting_description),
                OnboardingQuestion.Option(AWhile, R.string.onboarding_experience_a_while, R.string.onboarding_experience_a_while_description),
                OnboardingQuestion.Option(Years, R.string.onboarding_experience_years, R.string.onboarding_experience_years_description),
            ),
        ),
        OnboardingQuestion.Choice(
            id = Goal,
            label = R.string.onboarding_goal_label,
            title = R.string.onboarding_goal_title,
            options = listOf(
                OnboardingQuestion.Option(GetStronger, R.string.onboarding_goal_stronger, R.string.onboarding_goal_stronger_description),
                OnboardingQuestion.Option(BuildMuscle, R.string.onboarding_goal_muscle, R.string.onboarding_goal_muscle_description),
                OnboardingQuestion.Option(BuildEndurance, R.string.onboarding_goal_endurance, R.string.onboarding_goal_endurance_description),
                OnboardingQuestion.Option(StayFit, R.string.onboarding_goal_fit, R.string.onboarding_goal_fit_description),
            ),
        ),
        OnboardingQuestion.Reminders(
            id = Reminders,
            label = R.string.onboarding_reminders_label,
            title = R.string.onboarding_reminders_title,
            helper = R.string.onboarding_reminders_helper,
        ),
    )

    // Pounds in the few countries that lift in them, kilograms everywhere else
    fun deviceUnit(locale: Locale = Locale.getDefault()) =
        if (locale.country in setOf("US", "LR", "MM")) Pounds else Kilograms

    // What an answer becomes when its question is skipped
    fun skippedAnswer(questionId: String): Answer = when (questionId) {
        Units -> Answer.Choice(deviceUnit())
        Days -> Answer.Days(3)
        Where -> Answer.Choice(Gym)
        Experience -> Answer.Choice(JustStarting)
        Goal -> Answer.Choice(StayFit)
        else -> Answer.RemindersOff
    }

    // Training days spread across the week for a number of days a week
    fun trainingDays(count: Int): List<DayOfWeek> = when (count) {
        1 -> listOf(DayOfWeek.MONDAY)
        2 -> listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        3 -> listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        4 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        5 -> DayOfWeek.entries.take(5)
        6 -> DayOfWeek.entries.take(6)
        else -> DayOfWeek.entries
    }

    // The seven days in order, starting on the given day
    fun week(startingOn: DayOfWeek): List<DayOfWeek> = (0L until 7L).map { startingOn.plus(it) }

    fun formatTime(hour: Int, minute: Int) = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
}

fun DayOfWeek.shortName(): String = getDisplayName(TextStyle.SHORT, Locale.getDefault())

fun DayOfWeek.fullName(): String = getDisplayName(TextStyle.FULL, Locale.getDefault())
