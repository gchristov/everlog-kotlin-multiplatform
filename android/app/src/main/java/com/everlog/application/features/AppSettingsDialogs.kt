package com.everlog.application.features

import android.content.Context
import com.everlog.android.settings.ui.SettingsDialogs
import com.everlog.ui.dialog.AppDialogs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import rx.Observable

@ContributesBinding(AppScope::class)
@Inject
class AppSettingsDialogs : SettingsDialogs {
    override fun showWeightIncreasePrompt(context: Context, value: Float): Observable<Float> =
        AppDialogs.showWeightIncreaseDialog(context, value)

    override fun showWeeklyGoalPrompt(context: Context, value: Int): Observable<Int> =
        AppDialogs.showPickerNumberDialog(context, value, AppDialogs.NumberPickerDialogType.WEEKLY_GOAL)
}
