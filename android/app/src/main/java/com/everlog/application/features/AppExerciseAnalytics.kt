package com.everlog.application.features

import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.exercise.ExerciseAnalytics
import com.everlog.managers.analytics.AnalyticsManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppExerciseAnalytics : ExerciseAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun exerciseCreated() = analytics.exerciseCreated()

    override fun exerciseModified() = analytics.exerciseModified()

    override fun exerciseDeleted() = analytics.exerciseDeleted()

    override fun statisticsRangeModified(range: RangeType) = analytics.statisticsRangeModified(range.name)
}
