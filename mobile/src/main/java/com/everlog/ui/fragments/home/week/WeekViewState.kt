package com.everlog.ui.fragments.home.week

import com.everlog.data.controllers.statistics.UserStatsController
import com.everlog.data.model.plan.ELPlan
import com.everlog.data.model.plan.ELPlanState

// Single source of truth for the Week tab: each IWeekView derives its entire
// visibility tree from the state passed to render(), so loading/content/empty
// can never drift out of sync the way separate toggle calls could.
sealed class WeekViewState {
    data class Loading(val isPlan: Boolean) : WeekViewState()
    data class Stats(val stats: UserStatsController.StatsResult) : WeekViewState()
    data class Plan(val plan: ELPlan?, val state: ELPlanState?) : WeekViewState()
}
