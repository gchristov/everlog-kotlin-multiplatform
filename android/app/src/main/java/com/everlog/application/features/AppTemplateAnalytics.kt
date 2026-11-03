package com.everlog.application.features

import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.template.TemplateAnalytics
import com.everlog.managers.analytics.AnalyticsManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppTemplateAnalytics : TemplateAnalytics {
    override fun routineDeleted() = AnalyticsManager.manager.routineDeleted()

    override fun setTypeSelected(type: ELSetType?) = AnalyticsManager.manager.setTypeSelected(type)
}
