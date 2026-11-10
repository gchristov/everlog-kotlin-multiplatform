package com.everlog.application.features

import com.everlog.android.home.HomeAnalytics
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppHomeAnalytics : HomeAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun planDayCompleted() = analytics.planDayComplete()

    override fun planCompleted() = analytics.planCompleted()

    override fun notificationShown(title: String?) = analytics.notificationHomeShown(title)

    override fun notificationDismissed(title: String?) = analytics.notificationHomeDismissed(title)

    override fun notificationTapped(title: String?) = analytics.notificationHomeTapped(title)

    override fun startWorkoutTapped(fromEmptyWeek: Boolean) =
        if (fromEmptyWeek) analytics.homeAddWeekEmptyStateTapped() else analytics.homeAddFabTapped()

    override fun workoutResumedOnLaunch(setsCompleted: Int) =
        analytics.workoutResumed(AnalyticsConstants.WORKOUT_RESUMED_SOURCE_LAUNCH, setsCompleted)

    override fun ongoingWorkoutPromptShown(setsCompleted: Int) =
        analytics.workoutDiscardPromptShown(AnalyticsConstants.DISCARD_PROMPT_SOURCE_HOME, setsCompleted)

    override fun ongoingWorkoutPromptCancelled() = analytics.workoutDiscardPromptCancelled(AnalyticsConstants.DISCARD_PROMPT_SOURCE_HOME)

    override fun ongoingWorkoutDiscarded() = analytics.workoutStopped()
}
