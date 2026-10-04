package com.everlog.ui.activities.onboarding

import com.everlog.ui.activities.onboarding.OnboardingQuestions.BuildEndurance
import com.everlog.ui.activities.onboarding.OnboardingQuestions.BuildMuscle
import com.everlog.ui.activities.onboarding.OnboardingQuestions.Bodyweight
import com.everlog.ui.activities.onboarding.OnboardingQuestions.GetStronger
import com.everlog.ui.activities.onboarding.OnboardingQuestions.Gym
import com.everlog.ui.activities.onboarding.OnboardingQuestions.Home
import com.everlog.ui.activities.onboarding.OnboardingQuestions.JustStarting
import com.everlog.ui.activities.onboarding.OnboardingQuestions.Years
import org.threeten.bp.DayOfWeek

// The suggested week shown on the reveal, from the design's routine generation rules. Display only:
// nothing is saved as a routine yet. Exercise lists are placeholders until the real generator.

data class OnboardingWeek(
    val summary: String,
    val routines: List<Routine>,
    val experienced: Boolean,
) {
    data class Routine(
        val name: String,
        val days: List<DayOfWeek>,
        val exercises: List<Exercise>,
    ) {
        val sets: Int get() = exercises.sumOf { it.sets }
    }

    data class Exercise(
        val name: String,
        val sets: Int,
        val reps: String,
    )
}

internal fun buildWeek(answers: Map<String, Answer>): OnboardingWeek {
    val days = (answers[OnboardingQuestions.Days] as? Answer.Days)?.count ?: 3
    val where = answers.choice(OnboardingQuestions.Where) ?: Gym
    val experience = answers.choice(OnboardingQuestions.Experience) ?: JustStarting
    val goal = answers.choice(OnboardingQuestions.Goal) ?: OnboardingQuestions.StayFit

    // 1 day: Full body. 2 days: Full body A / B. 3 days: Full body A / B / C under 6 months,
    // otherwise Push / Pull / Legs. 4 days: Upper / Lower. 5 to 7 days: Push / Pull / Legs, repeated
    // from the fourth day.
    val split = when {
        days == 1 -> listOf("Full body")
        days == 2 -> listOf("Full body A", "Full body B")
        days == 3 && experience == JustStarting -> listOf("Full body A", "Full body B", "Full body C")
        days == 4 -> listOf("Upper", "Lower")
        else -> listOf("Push", "Pull", "Legs")
    }
    // e.g. "Full body A / B / C", "Upper / Lower"
    val splitName = if (split.size > 1 && split.first().startsWith("Full body")) {
        "Full body " + split.joinToString(" / ") { it.removePrefix("Full body ") }
    } else {
        split.joinToString(" / ")
    }

    // Each training day takes the next routine in the split
    val trainingDays = OnboardingQuestions.trainingDays(days)
    val routines = split.map { name ->
        OnboardingWeek.Routine(
            name = name,
            days = trainingDays.filterIndexed { index, _ -> split[index % split.size] == name },
            exercises = exercises(name, where).mapIndexed { index, exercise ->
                val (sets, reps) = setsAndReps(goal, isMainLift = index < 2)
                OnboardingWeek.Exercise(exercise, sets, reps)
            },
        )
    }

    val place = when (where) {
        Home -> "Home dumbbells"
        Bodyweight -> "Bodyweight"
        else -> "Gym"
    }
    val goalName = when (goal) {
        GetStronger -> "Get stronger"
        BuildMuscle -> "Build muscle"
        BuildEndurance -> "Build endurance"
        else -> "Stay fit"
    }
    return OnboardingWeek(
        summary = "$days ${if (days == 1) "day" else "days"} · $splitName · $place · $goalName",
        routines = routines,
        experienced = experience == Years,
    )
}

// Get stronger 4 × 4–6 on main lifts · Build muscle 3 × 8–12 · Build endurance 3 × 15–20 · Stay fit
// 3 × 10–12
private fun setsAndReps(goal: String, isMainLift: Boolean): Pair<Int, String> = when (goal) {
    GetStronger -> if (isMainLift) 4 to "4–6" else 3 to "6–8"
    BuildMuscle -> 3 to "8–12"
    BuildEndurance -> 3 to "15–20"
    else -> 3 to "10–12"
}

// 4 to 7 exercises a day, only from the equipment chosen
private fun exercises(routine: String, where: String): List<String> = when (where) {
    Home -> when (routine) {
        "Upper", "Push" -> listOf("Dumbbell bench press", "Dumbbell shoulder press", "Dumbbell fly", "Lateral raise", "Overhead triceps extension")
        "Pull" -> listOf("One-arm dumbbell row", "Dumbbell pullover", "Reverse fly", "Hammer curl", "Dumbbell shrug")
        "Lower", "Legs" -> listOf("Goblet squat", "Dumbbell Romanian deadlift", "Bulgarian split squat", "Dumbbell step-up", "Calf raise")
        else -> listOf("Goblet squat", "Dumbbell bench press", "One-arm dumbbell row", "Dumbbell Romanian deadlift", "Dumbbell shoulder press")
    }
    Bodyweight -> when (routine) {
        "Upper", "Push" -> listOf("Push-up", "Pike push-up", "Dips", "Diamond push-up", "Plank")
        "Pull" -> listOf("Pull-up", "Inverted row", "Chin-up", "Superman", "Hollow hold")
        "Lower", "Legs" -> listOf("Squat", "Reverse lunge", "Glute bridge", "Single-leg calf raise", "Wall sit")
        else -> listOf("Squat", "Push-up", "Inverted row", "Reverse lunge", "Plank")
    }
    else -> when (routine) {
        "Upper" -> listOf("Bench press", "Barbell row", "Overhead press", "Lat pulldown", "Lateral raise", "Triceps pushdown")
        "Lower" -> listOf("Back squat", "Romanian deadlift", "Leg press", "Leg curl", "Standing calf raise")
        "Push" -> listOf("Bench press", "Overhead press", "Incline dumbbell press", "Dips", "Lateral raise")
        "Pull" -> listOf("Deadlift", "Pull-up", "Barbell row", "Face pull")
        "Legs" -> listOf("Back squat", "Romanian deadlift", "Leg press", "Leg curl", "Standing calf raise")
        "Full body B" -> listOf("Deadlift", "Overhead press", "Lat pulldown", "Leg press", "Plank")
        "Full body C" -> listOf("Front squat", "Incline dumbbell press", "Seated cable row", "Hip thrust", "Face pull")
        else -> listOf("Back squat", "Bench press", "Barbell row", "Romanian deadlift", "Lateral raise")
    }
}

private fun Map<String, Answer>.choice(id: String) = (get(id) as? Answer.Choice)?.optionId
