package com.everlog.config

import androidx.core.content.ContextCompat
import com.everlog.BuildConfig
import com.everlog.R
import com.everlog.application.ELApplication
import com.everlog.managers.ErrorManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.fragments.home.activity.history.HistoryHomeFragment
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment

class AppConfig {

    val defaultRestTimeSeconds = 60
    val defaultRestTimeOffsetSeconds = 10
    val defaultExerciseTimeSeconds = 30

    val defaultStatsRange = StatisticsHomeFragment.RangeType.WEEK
    val defaultStatsExerciseRange = StatisticsHomeFragment.RangeType.MONTH

    val defaultHistoryViewType = HistoryHomeFragment.ViewType.CALENDAR

    val maxPlanWeeks = 20
    val maxPlanWeekDaysFree = 1
    val maxExerciseSelection = 10
    val maxExerciseSets = 14

    val rateTriggerModLaunchNumber = 20
    val rateTriggerModConsecutiveLaunchDays = 3
    val rateTriggerLastShownDelayDays = 21

    companion object {

        @JvmField
        val configuration = AppConfig()
    }

    fun configureApp() {
        configureAnalyticsManager()
        configureErrorManager()
    }

    private fun configureErrorManager() {
        ErrorManager.manager.initialize()
    }

    private fun configureAnalyticsManager() {
        AnalyticsManager.manager.initialize()
    }
}