package com.everlog.application.features

import android.content.Context
import com.everlog.android.settings.ui.SetSuggestionText
import com.everlog.android.settings.ui.SettingsConfig
import com.everlog.android.settings.data.MuscleGoal
import com.everlog.android.workout.setsuggestions.SetSuggestion
import com.everlog.managers.RemoteConfigManager
import com.everlog.utils.device.DeviceInfo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppSettingsConfig : SettingsConfig {
    override fun setSuggestionsV2(): Boolean = RemoteConfigManager.manager.setSuggestionsV2()

    override fun setSuggestionText(context: Context, goal: MuscleGoal): SetSuggestionText? =
        SetSuggestion.of(goal)?.let { SetSuggestionText(context.getString(it.title), it.subtitle(context)) }

    override fun appInfo(): String = DeviceInfo.appInfo
}
