package com.everlog.android.workout.data.exercisehistory

import com.everlog.android.common.persistence.PreferencesManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

// The range every exercise's statistics open on: the one the user last picked, this month until they pick one
interface ExerciseStatsPreferences {
    fun range(): ExerciseStatsRange

    fun setRange(range: ExerciseStatsRange)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ExerciseStatsPreferences>())
@Inject
class RealExerciseStatsPreferences : PreferencesManager(), ExerciseStatsPreferences {

    // A name it doesn't know, e.g. from a later version, opens this month
    override fun range(): ExerciseStatsRange =
        ExerciseStatsRange.entries.firstOrNull { it.name == getPreference(RANGE, null as String?) } ?: ExerciseStatsRange.Month

    override fun setRange(range: ExerciseStatsRange) = savePreference(range.name, RANGE)

    private companion object {
        const val RANGE = "EXERCISE_STATISTICS_RANGE"
    }
}
