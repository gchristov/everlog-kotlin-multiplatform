package com.everlog.ui.activities.onboarding

import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.TextStyle
import java.util.Locale

// The setup questions from the Everlog Onboarding design's copy deck. Copy is hardcoded while this is
// a prototype.

sealed interface OnboardingQuestion {
    val id: String
    // Shown on the collapsed summary row
    val label: String
    val title: String
    val helper: String?

    data class Choice(
        override val id: String,
        override val label: String,
        override val title: String,
        override val helper: String? = null,
        val options: List<Option>,
        // Two large cards side by side (units) instead of a list of answer cards
        val sideBySide: Boolean = false,
    ) : OnboardingQuestion

    data class Days(
        override val id: String,
        override val label: String,
        override val title: String,
        override val helper: String?,
        val options: List<Int>,
    ) : OnboardingQuestion

    data class Reminders(
        override val id: String,
        override val label: String,
        override val title: String,
        override val helper: String?,
    ) : OnboardingQuestion

    data class Option(
        val id: String,
        val title: String,
        val description: String? = null,
        // How the answer reads on the summary row
        val summary: String = title,
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
            label = "Units",
            title = "Which units do you lift in?",
            helper = "Set from your device. You can change this later in Settings.",
            options = listOf(
                OnboardingQuestion.Option(Kilograms, "kg", "Kilograms", summary = "Kilograms"),
                OnboardingQuestion.Option(Pounds, "lb", "Pounds", summary = "Pounds"),
            ),
            sideBySide = true,
        ),
        OnboardingQuestion.Days(
            id = Days,
            label = "Training days",
            title = "How many days a week do you train?",
            helper = "We'll build your week around this.",
            options = (1..7).toList(),
        ),
        OnboardingQuestion.Choice(
            id = Where,
            label = "Where you train",
            title = "Where do you train?",
            options = listOf(
                OnboardingQuestion.Option(Gym, "Gym", "Barbells, machines, cables", summary = "Gym"),
                OnboardingQuestion.Option(Home, "Home with dumbbells", "Dumbbells and a bit of space", summary = "Home, dumbbells"),
                OnboardingQuestion.Option(Bodyweight, "Bodyweight only", "No equipment needed", summary = "Bodyweight"),
            ),
        ),
        OnboardingQuestion.Choice(
            id = Experience,
            label = "Experience",
            title = "How long have you been lifting?",
            options = listOf(
                OnboardingQuestion.Option(JustStarting, "Just starting", "Under 6 months"),
                OnboardingQuestion.Option(AWhile, "A while", "6 months to 2 years"),
                OnboardingQuestion.Option(Years, "Years", "2+ years"),
            ),
        ),
        OnboardingQuestion.Choice(
            id = Goal,
            label = "Main goal",
            title = "What's your main goal?",
            options = listOf(
                OnboardingQuestion.Option(GetStronger, "Get stronger", "Heavier lifts, fewer reps"),
                OnboardingQuestion.Option(BuildMuscle, "Build muscle", "Size and shape, steady volume"),
                OnboardingQuestion.Option(BuildEndurance, "Build endurance", "Higher reps, shorter rests"),
                OnboardingQuestion.Option(StayFit, "Stay fit", "A balanced week you can keep up"),
            ),
        ),
        OnboardingQuestion.Reminders(
            id = Reminders,
            label = "Reminders",
            title = "Want a reminder on training days?",
            helper = "We'll only message you on the days you pick. Android will ask to allow notifications when you tap Remind me.",
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

    fun summary(question: OnboardingQuestion, answer: Answer, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): String = when (answer) {
        is Answer.Choice -> (question as? OnboardingQuestion.Choice)?.options
            ?.firstOrNull { it.id == answer.optionId }?.summary ?: ""
        is Answer.Days -> "${answer.count} ${if (answer.count == 1) "day" else "days"} a week"
        is Answer.Reminders -> week(startingOn = firstDayOfWeek).filter { it in answer.days }.joinToString(", ") { it.shortName() } +
                " at ${formatTime(answer.hour, answer.minute)}"
        Answer.RemindersOff -> "Off"
    }

    fun formatTime(hour: Int, minute: Int) = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
}

fun DayOfWeek.shortName(): String = getDisplayName(TextStyle.SHORT, Locale.getDefault())

fun DayOfWeek.fullName(): String = getDisplayName(TextStyle.FULL, Locale.getDefault())
