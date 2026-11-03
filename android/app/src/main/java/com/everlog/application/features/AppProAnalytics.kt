package com.everlog.application.features

import com.everlog.android.pro.ProAnalytics
import com.everlog.managers.analytics.AnalyticsManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppProAnalytics : ProAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun buyMonth() = analytics.proBuyMonth()

    override fun buyYear() = analytics.proBuyYear()

    override fun purchased() = analytics.proBuyPurchased()

    override fun cancelled() = analytics.proBuyCancelled()

    override fun alreadyOwned() = analytics.proBuyAlreadyOwned()

    override fun restore() = analytics.proBuyRestore()
}
