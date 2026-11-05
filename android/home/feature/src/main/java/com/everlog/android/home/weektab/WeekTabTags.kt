package com.everlog.android.home.weektab

// For tests to find the Week tab's parts
object WeekTabTags {
    const val Days = "weekTab.days"
    const val Goal = "weekTab.goal"
    const val Loading = "weekTab.loading"
    const val Empty = "weekTab.empty"
    const val Banner = "weekTab.banner"
    const val BannerClose = "weekTab.bannerClose"
    const val Plan = "weekTab.plan"
    const val SkipDay = "weekTab.skipDay"
    const val StartDay = "weekTab.startDay"
    const val FinishPlan = "weekTab.finishPlan"

    fun planDay(number: Int) = "weekTab.planDay.$number"
}
