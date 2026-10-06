package com.everlog.data.controllers.starterroutines

import com.everlog.data.controllers.starterroutines.StarterProfile.Experience
import com.everlog.data.controllers.starterroutines.StarterProfile.Goal
import com.everlog.data.controllers.starterroutines.StarterRoutine.Target
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.set.ELSetType
import org.threeten.bp.DayOfWeek
import timber.log.Timber
import java.util.UUID

/**
 * How someone trains, from the onboarding questions (TAS-442).
 */
data class StarterProfile(
    val daysPerWeek: Int,
    val place: Place,
    val experience: Experience,
    val goal: Goal,
) {
    enum class Place { GYM, HOME_DUMBBELLS, BODYWEIGHT }
    enum class Experience { JUST_STARTING, A_WHILE, YEARS }
    enum class Goal { GET_STRONGER, BUILD_MUSCLE, BUILD_ENDURANCE, STAY_FIT }
}

/**
 * The suggested week: one routine per day type in the split, each with the days it's trained on.
 */
data class StarterWeek(
    val split: Split,
    val routines: List<StarterRoutine>,
) {
    enum class Split { FULL_BODY, UPPER_LOWER, PUSH_PULL_LEGS }
}

data class StarterRoutine(
    val name: String,
    val days: List<DayOfWeek>,
    val exercises: List<Exercise>,
    val restTimeSeconds: Int,
) {
    data class Exercise(
        // A global exercise's uuid, from the exercise library
        val exerciseId: String,
        val name: String,
        val sets: Int,
        val target: Target,
    )

    sealed interface Target {
        data class Reps(val range: IntRange) : Target
        data class Time(val seconds: Int) : Target
    }

    /**
     * A routine of single sets, with its exercises from [library]. Each set's required reps are
     * the top of the exercise's rep range. An exercise the library doesn't have is left out and
     * reported as a non-fatal error, as it means the template points at the wrong uuid.
     */
    fun toRoutine(library: Collection<ELExercise>, createdDate: Long): ELRoutine {
        val byId = library.filter { it.uuid != null }.associateBy { it.uuid!! }
        val groups = exercises.mapNotNull { exercise ->
            val match = byId[exercise.exerciseId]
            if (match == null) {
                Timber.tag(TAG).e(ExerciseNotFoundException(name, exercise))
                return@mapNotNull null
            }
            val sets = List(exercise.sets) {
                when (val target = exercise.target) {
                    is Target.Reps -> ELSet(requiredReps = target.range.last)
                    is Target.Time -> ELSet(requiredTimeSeconds = target.seconds)
                }
            }
            ELExerciseGroup(
                uuid = UUID.randomUUID().toString(),
                type = ELSetType.SINGLE.name,
                exercises = mutableListOf(ELRoutineExercise(UUID.randomUUID().toString(), match, sets.toMutableList())),
                restTimeSeconds = restTimeSeconds,
            )
        }
        return ELRoutine(
            uuid = UUID.randomUUID().toString(),
            name = name,
            exerciseGroups = groups.toMutableList(),
            createdDate = createdDate,
        )
    }

    class ExerciseNotFoundException(routine: String, exercise: Exercise) :
        IllegalStateException("$routine: ${exercise.name} (${exercise.exerciseId}) isn't in the exercise library")

    private companion object {
        const val TAG = "StarterRoutine"
    }
}

/**
 * Builds a starter week from the onboarding answers, following the rules in the Everlog Onboarding
 * design (TAS-442).
 */
object StarterRoutineGenerator {

    fun generate(profile: StarterProfile): StarterWeek {
        require(profile.daysPerWeek in 1..7) { "Days per week must be 1 to 7, was ${profile.daysPerWeek}" }
        val (split, dayTypes) = split(profile.daysPerWeek, profile.experience)
        val trainingDays = trainingDays(profile.daysPerWeek)
        // Just starting 4 exercises a day, a while 5, years 6
        val exerciseCount = when (profile.experience) {
            Experience.JUST_STARTING -> 4
            Experience.A_WHILE -> 5
            Experience.YEARS -> 6
        }
        val routines = dayTypes.mapIndexed { index, dayType ->
            StarterRoutine(
                name = dayType.routineName,
                // Each training day takes the next routine in the split
                days = trainingDays.filterIndexed { dayIndex, _ -> dayIndex % dayTypes.size == index },
                exercises = StarterExercises.forDay(profile.place, dayType.exercises)
                    .take(exerciseCount)
                    .mapIndexed { exerciseIndex, exercise ->
                        exercise(exercise, profile.goal, isMainLift = exerciseIndex < 2)
                    },
                restTimeSeconds = restTimeSeconds(profile.goal),
            )
        }
        return StarterWeek(split, routines)
    }

    /**
     * Training days spread across the week for a number of days a week.
     */
    fun trainingDays(count: Int): List<DayOfWeek> = when (count) {
        1 -> listOf(DayOfWeek.MONDAY)
        2 -> listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        3 -> listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        4 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        5 -> DayOfWeek.entries.take(5)
        6 -> DayOfWeek.entries.take(6)
        else -> DayOfWeek.entries
    }

    private data class DayType(val routineName: String, val exercises: StarterExercises.Day)

    // 1 day: Full body. 2 days: Full body A / B. 3 days: Full body A / B / C under 6 months,
    // otherwise Push / Pull / Legs. 4 days: Upper / Lower. 5 to 7 days: Push / Pull / Legs, repeated
    // from the fourth day.
    private fun split(days: Int, experience: Experience): Pair<StarterWeek.Split, List<DayType>> {
        val fullBodyA = DayType("Full body A", StarterExercises.Day.FULL_BODY_A)
        val fullBodyB = DayType("Full body B", StarterExercises.Day.FULL_BODY_B)
        val fullBodyC = DayType("Full body C", StarterExercises.Day.FULL_BODY_C)
        return when {
            days == 1 -> StarterWeek.Split.FULL_BODY to listOf(DayType("Full body", StarterExercises.Day.FULL_BODY_A))
            days == 2 -> StarterWeek.Split.FULL_BODY to listOf(fullBodyA, fullBodyB)
            days == 3 && experience == Experience.JUST_STARTING -> StarterWeek.Split.FULL_BODY to listOf(fullBodyA, fullBodyB, fullBodyC)
            days == 4 -> StarterWeek.Split.UPPER_LOWER to listOf(
                DayType("Upper", StarterExercises.Day.UPPER),
                DayType("Lower", StarterExercises.Day.LOWER),
            )
            else -> StarterWeek.Split.PUSH_PULL_LEGS to listOf(
                DayType("Push", StarterExercises.Day.PUSH),
                DayType("Pull", StarterExercises.Day.PULL),
                DayType("Legs", StarterExercises.Day.LEGS),
            )
        }
    }

    // Get stronger 4 × 4–6 on the main lifts and 3 × 6–8 after · Build muscle 3 × 8–12 · Build
    // endurance 3 × 15–20 · Stay fit 3 × 10–12. Holds are 30 seconds, 45 for endurance.
    private fun exercise(exercise: StarterExercises.Exercise, goal: Goal, isMainLift: Boolean): StarterRoutine.Exercise {
        val sets = if (goal == Goal.GET_STRONGER && isMainLift && !exercise.hold) 4 else 3
        val target = when {
            exercise.hold -> Target.Time(if (goal == Goal.BUILD_ENDURANCE) 45 else 30)
            goal == Goal.GET_STRONGER -> Target.Reps(if (isMainLift) 4..6 else 6..8)
            goal == Goal.BUILD_MUSCLE -> Target.Reps(8..12)
            goal == Goal.BUILD_ENDURANCE -> Target.Reps(15..20)
            else -> Target.Reps(10..12)
        }
        return StarterRoutine.Exercise(exercise.uuid, exercise.name, sets, target)
    }

    private fun restTimeSeconds(goal: Goal) = when (goal) {
        Goal.GET_STRONGER -> 180
        Goal.BUILD_MUSCLE -> 90
        Goal.BUILD_ENDURANCE -> 45
        Goal.STAY_FIT -> 60
    }
}
