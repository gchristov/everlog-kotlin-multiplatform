package com.everlog.android.workout.testfixtures

import com.everlog.android.workout.data.exercisehistory.ExerciseStatsPreferences
import com.everlog.android.workout.data.exercisehistory.ExerciseStatsRange

// The range as the test sets it, and what the screen saved
class FakeExerciseStatsPreferences(private var saved: ExerciseStatsRange = ExerciseStatsRange.Month) : ExerciseStatsPreferences {
    override fun range() = saved

    override fun setRange(range: ExerciseStatsRange) {
        saved = range
    }
}
