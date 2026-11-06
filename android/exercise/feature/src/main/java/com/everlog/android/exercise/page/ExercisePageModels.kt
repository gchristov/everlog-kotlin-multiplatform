package com.everlog.android.exercise.page

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.exercise.data.model.library.ExerciseGuide
import com.everlog.android.workout.data.exercisehistory.ExerciseSession
import com.everlog.android.workout.data.exercisehistory.ExerciseStats
import com.everlog.android.workout.data.exercisehistory.ExerciseStatsRange
import org.threeten.bp.LocalDate

enum class ExercisePageTab {
    Info,
    Statistics,
    History,
}

/**
 * What the page shows.
 *
 * @param exercise As it is now: a user's own exercise edited while the page is open shows the edit
 * @param own The user's own exercise, which they can edit and delete
 * @param stats The [range]'s, from the last 12 months
 * @param sessions Each time the exercise was done, most recently first, as far back as the user has scrolled
 * @param loading Until the last 12 months first load
 * @param failed The last 12 months couldn't load, as their first sync didn't finish: try again
 * @param loadingMore An older year is being read
 * @param complete The history goes back to the first workout
 * @param olderFailed An older year couldn't be read: scrolling to the end again tries again
 * @param feedback There's a feedback form to ask for more statistics with
 * @param statsExample Sample statistics to show, faded, while the range has no sessions
 * @param historyExample Sample sessions to show, faded, once the history turns out to have none
 */
data class ExercisePageState(
    val exercise: ELExercise,
    val own: Boolean,
    val guide: ExerciseGuide = ExerciseGuide(),
    val range: ExerciseStatsRange,
    val today: LocalDate,
    val weightUnit: WeightUnit,
    val stats: ExerciseStats,
    val sessions: List<ExerciseSession> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false,
    val loadingMore: Boolean = false,
    val complete: Boolean = false,
    val olderFailed: Boolean = false,
    val feedback: Boolean = false,
    val statsExample: ExerciseStats? = null,
    val historyExample: List<ExerciseSession>? = null,
    val deletePrompt: Boolean = false,
    val deleteFailed: Boolean = false,
    val closed: Boolean = false,
)
