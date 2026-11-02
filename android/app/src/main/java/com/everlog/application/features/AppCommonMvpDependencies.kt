package com.everlog.application.features

import com.everlog.application.ELApplication
import com.everlog.android.common.mvp.CommonMvpDependencies
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.utils.device.DeviceUtils

class AppCommonMvpDependencies : CommonMvpDependencies {

    override fun screenViewed(name: String) = AnalyticsManager.manager.screenName(name)

    override fun hasUser(): Boolean = ELApplication.graph.userSession.user() != null

    override fun isTestLabRun(): Boolean = DeviceUtils.isFirebaseTestLabRun()
}
