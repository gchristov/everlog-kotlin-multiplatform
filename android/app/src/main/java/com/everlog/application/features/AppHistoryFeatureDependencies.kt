package com.everlog.application.features

import android.content.Context
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.history.HistoryFeatureDependencies
import com.everlog.config.FeedbackSource
import com.everlog.managers.RemoteConfigManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.navigator.ELNavigator

class AppHistoryFeatureDependencies : HistoryFeatureDependencies {

    override fun openWorkoutDetails(context: Context, workout: ELWorkout) =
        ELNavigator(context).openWorkoutDetails(workout, false, false)

    override fun hasStatisticsFeedback(): Boolean =
        RemoteConfigManager.manager.feedbackFormUrl(FeedbackSource.ACTIVITY_STATISTICS) != null

    override fun openStatisticsFeedback(context: Context) = ELNavigator(context).openFeedbackForm(FeedbackSource.ACTIVITY_STATISTICS)

    override fun statisticsRangeModified(range: RangeType) = AnalyticsManager.manager.statisticsRangeModified(range)
}
