package com.everlog.application.features

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.history.statistics.UserStatsController
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.workout.WorkoutStats
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppWorkoutStats(private val settings: UserSettings) : WorkoutStats {
    override fun workoutsThisWeek(workouts: List<ELWorkout>, onCount: (Int) -> Unit, onError: (Throwable) -> Unit) =
        UserStatsController(settings.weightUnit(), settings.firstDayOfWeek()).loadStats(RangeType.WEEK, workouts, object : UserStatsController.OnCompleteListener {
            override fun onComplete(result: UserStatsController.StatsResult) = onCount(result.workoutsCompleted)

            override fun onError(throwable: Throwable) = onError(throwable)
        })
}
