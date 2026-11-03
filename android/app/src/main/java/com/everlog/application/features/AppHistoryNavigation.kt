package com.everlog.application.features

import android.content.Context
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.history.HistoryNavigation
import com.everlog.config.FeedbackSource
import com.everlog.managers.RemoteConfigManager
import com.everlog.ui.navigator.ELNavigator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppHistoryNavigation : HistoryNavigation {
    override fun openWorkoutDetails(context: Context, workout: ELWorkout) =
        ELNavigator(context).openWorkoutDetails(workout, false, false)

    override fun hasStatisticsFeedback(): Boolean =
        RemoteConfigManager.manager.feedbackFormUrl(FeedbackSource.ACTIVITY_STATISTICS) != null

    override fun openStatisticsFeedback(context: Context) = ELNavigator(context).openFeedbackForm(FeedbackSource.ACTIVITY_STATISTICS)
}
