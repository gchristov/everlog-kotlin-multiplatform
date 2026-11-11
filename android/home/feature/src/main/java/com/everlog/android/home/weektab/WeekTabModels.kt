package com.everlog.android.home.weektab

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.home.data.week.WeekStats
import org.threeten.bp.LocalDate

data class WeekTabState(
    val today: LocalDate,
    val stats: WeekStats,
    val weeklyGoal: Int,
    val weightUnit: WeightUnit,
    // Until the week's workouts first load; the days show already
    val loading: Boolean = true,
    val banner: WeekTabBanner? = null,
    // The plan the user is following, which the tab shows instead of the week
    val plan: WeekTabPlan? = null,
    val prompt: WeekTabPrompt? = null,
) {
    val empty: Boolean get() = !loading && stats.workouts == 0

    // With nothing done this week, made-up numbers that show what the cards will count
    val example: WeekTabState? get() = if (empty) copy(stats = exampleWeekStats(stats, weeklyGoal, weightUnit)) else null

    val workoutsLeft: Int get() = (weeklyGoal - stats.workouts).coerceAtLeast(0)

    // Of the weekly goal, from 0 to 1
    val goalProgress: Float get() = if (weeklyGoal <= 0) 1f else (stats.workouts.toFloat() / weeklyGoal).coerceAtMost(1f)
}

data class WeekTabPlan(
    val uuid: String?,
    val name: String,
    // Of the plan's days done, from 0 to 100
    val progress: Int,
    // From 1
    val weekNumber: Int,
    val weekCount: Int,
    val days: List<WeekTabPlanDay>,
) {
    // Every day is done, so the plan can be finished
    val done: Boolean get() = days.none { it.next }
}

data class WeekTabPlanDay(
    // From 1, within the week
    val number: Int,
    // Null for a rest day, or a day with nothing set
    val routine: ELRoutine?,
    val complete: Boolean,
    val next: Boolean,
)

enum class WeekTabPrompt {
    SkipDay,
    RestDay,
}

// What starting the plan's next day opens, if anything
sealed interface WeekTabPlanStart {
    data class Workout(val routine: ELRoutine) : WeekTabPlanStart

    data object Finished : WeekTabPlanStart

    // A rest day, marked done in place
    data object Nothing : WeekTabPlanStart
}

data class WeekTabBanner(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val updateRequired: Boolean,
)

// One workout short of the goal, so the ring shows how it fills, each of them about an hour, every other day from
// the week's start
internal fun exampleWeekStats(week: WeekStats, weeklyGoal: Int, unit: WeightUnit): WeekStats {
    val workouts = (weeklyGoal - 1).coerceAtLeast(1)
    val spread = week.days.filterIndexed { index, _ -> index % 2 == 0 } + week.days.filterIndexed { index, _ -> index % 2 == 1 }
    return week.copy(
        workoutDays = spread.take(workouts).toSet(),
        workouts = workouts,
        weightLifted = workouts * if (unit == WeightUnit.POUND) ExamplePoundsPerWorkout else ExampleKilogramsPerWorkout,
        totalMillis = workouts * ExampleMillisPerWorkout,
        averageMillis = ExampleMillisPerWorkout,
    )
}

private const val ExampleKilogramsPerWorkout = 3_150f
private const val ExamplePoundsPerWorkout = 6_945f
private const val ExampleMillisPerWorkout = 65 * 60_000L
