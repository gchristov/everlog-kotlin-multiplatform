package com.everlog.application.features

import android.content.Context
import com.everlog.android.auth.AuthAppState
import com.everlog.android.settings.data.RealUserSettings
import com.everlog.ui.navigator.ELNavigator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppAuthAppState(private val settings: RealUserSettings) : AuthAppState {
    override fun onboardingPending(): Boolean = settings.onboardingPending()

    override fun setOnboardingPending(pending: Boolean) = settings.setOnboardingPending(pending)

    override fun cancelAppUseReminder(context: Context) = ELNavigator(context).cancelAppUseNotification()
}
