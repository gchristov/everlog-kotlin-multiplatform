package com.everlog.application.features

import com.everlog.BuildConfig
import com.everlog.android.home.HomeConfig
import com.everlog.android.home.data.model.HomeNotification
import com.everlog.managers.RemoteConfigManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppHomeConfig : HomeConfig {
    override fun notification(): HomeNotification? = RemoteConfigManager.manager.notificationHome()

    override val appVersionCode: Int = BuildConfig.VERSION_CODE
}
