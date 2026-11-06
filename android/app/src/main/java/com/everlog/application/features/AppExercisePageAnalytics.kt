package com.everlog.application.features

import com.everlog.android.exercise.ExerciseScreenNames
import com.everlog.android.exercise.page.ExercisePageAnalytics
import com.everlog.android.exercise.page.ExercisePageTab
import com.everlog.android.workout.data.exercisehistory.ExerciseStatsRange
import com.everlog.managers.analytics.AnalyticsManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppExercisePageAnalytics : ExercisePageAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun screenViewed() = analytics.screenName(ExerciseScreenNames.DETAILS_V2)

    override fun tabViewed(tab: ExercisePageTab) = analytics.screenName(
        when (tab) {
            ExercisePageTab.Info -> ExerciseScreenNames.INFO_V2
            ExercisePageTab.Statistics -> ExerciseScreenNames.STATISTICS_V2
            ExercisePageTab.History -> ExerciseScreenNames.HISTORY_V2
        },
    )

    // The old screen's names for the ranges it shares, as the Activity tab's for the 12 months
    override fun rangePicked(range: ExerciseStatsRange) = analytics.statisticsRangeModified(
        when (range) {
            ExerciseStatsRange.Today -> "TODAY"
            ExerciseStatsRange.Week -> "WEEK"
            ExerciseStatsRange.Month -> "MONTH"
            ExerciseStatsRange.Last12Months -> "LAST_12_MONTHS"
        },
    )

    override fun exerciseDeleted() = analytics.exerciseDeleted()
}
