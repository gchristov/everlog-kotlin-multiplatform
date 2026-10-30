package com.everlog.application.features

import com.everlog.android.common.mvp.CommonMvpDependencies
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.auth.LocalUserManager

class AppCommonMvpDependencies : CommonMvpDependencies {

    override fun screenViewed(name: String) = AnalyticsManager.manager.screenName(name)

    override fun hasUser(): Boolean = LocalUserManager.hasUser()
}
