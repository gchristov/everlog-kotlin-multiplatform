package com.everlog.android.settings.ui.tab

import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.mvvm.CommonViewModel
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.settings.ui.SettingsAnalytics
import com.everlog.android.settings.ui.SettingsConfig
import kotlinx.coroutines.CoroutineDispatcher
import org.threeten.bp.DayOfWeek

/**
 * Home's Settings tab: the signed-in account and the user's settings, all kept on the device, read again on [refresh]
 * when the tab shows or Remote Config changes, as other screens change some of them (Set suggestions the goal).
 *
 * @param canSendFeedback Whether Remote Config offers Settings a feedback form.
 * @param onSettingsChanged Tells the other screens a setting they show changed (the weight unit, the first day of the
 * week, the weekly goal).
 */
class SettingsTabViewModel(
    dispatcher: CoroutineDispatcher,
    private val settings: UserSettings,
    private val session: UserSession,
    private val config: SettingsConfig,
    private val analytics: SettingsAnalytics,
    private val canSendFeedback: () -> Boolean,
    private val showDeveloper: Boolean,
    private val onSettingsChanged: () -> Unit,
) : CommonViewModel<SettingsTabState>(dispatcher, read(settings, session, config, canSendFeedback(), showDeveloper)) {

    fun refresh() {
        setState { read(settings, session, config, canSendFeedback(), showDeveloper).copy(sheet = sheet) }
    }

    fun onFirstDayOfWeekTapped() = show(SettingsTabSheet.FirstDayOfWeek)

    fun onFirstDayOfWeekPicked(day: DayOfWeek) {
        settings.setFirstDayOfWeek(day)
        changed()
        analytics.firstWeekDayModified(day.name)
    }

    fun onWeightUnitTapped() = show(SettingsTabSheet.WeightUnit)

    fun onWeightUnitPicked(unit: WeightUnit) {
        settings.setWeightUnit(unit)
        changed()
        analytics.weightUnitModified(unit.name)
    }

    fun onWeeklyGoalTapped() = show(SettingsTabSheet.WeeklyGoal(settings.weeklyWorkoutsGoal().coerceIn(SettingsTabLimits.WeeklyGoals)))

    fun onWeeklyGoalPicked(value: Int) {
        if (state.value.sheet !is SettingsTabSheet.WeeklyGoal) return
        show(SettingsTabSheet.WeeklyGoal(value.coerceIn(SettingsTabLimits.WeeklyGoals)))
    }

    fun onWeeklyGoalSaved(value: Int) {
        settings.setWeeklyWorkoutsGoal(value)
        changed()
        analytics.weeklyGoalModified(value)
    }

    fun onWeightIncreaseTapped() = show(SettingsTabSheet.WeightIncrease(weightIncreaseQuarters(settings.weightIncrease())))

    fun onWeightIncreasePicked(whole: Int, quarter: Int) {
        if (state.value.sheet !is SettingsTabSheet.WeightIncrease) return
        show(SettingsTabSheet.WeightIncrease((whole * 4 + quarter).coerceIn(SettingsTabLimits.WeightIncreaseQuarters)))
    }

    fun onWeightIncreaseSaved(quarters: Int) {
        val value = quarters / 4f
        settings.setWeightIncrease(value)
        dismissAndRead()
        analytics.weightIncreaseModified(value)
    }

    fun onKeepScreenOnTapped() {
        val value = !settings.keepScreenOn()
        settings.setKeepScreenOn(value)
        dismissAndRead()
        analytics.keepScreenOnModified(value)
    }

    fun onLogOutTapped() = show(SettingsTabSheet.Logout)

    fun onSheetDismissed() {
        setState { copy(sheet = null) }
    }

    private fun show(sheet: SettingsTabSheet) {
        setState { copy(sheet = sheet) }
    }

    private fun changed() {
        dismissAndRead()
        onSettingsChanged()
    }

    private fun dismissAndRead() {
        setState { read(settings, session, config, canSendFeedback(), showDeveloper) }
    }
}

private fun read(
    settings: UserSettings,
    session: UserSession,
    config: SettingsConfig,
    showFeedback: Boolean,
    showDeveloper: Boolean,
): SettingsTabState {
    val user = session.user()
    return SettingsTabState(
        account = user?.takeUnless { it.isAnonymous() }?.let { SettingsAccountInfo(name = it.getFirstName(), photoUrl = it.photoUrl) },
        firstDayOfWeek = settings.firstDayOfWeek(),
        weightUnit = settings.weightUnit(),
        weeklyGoal = settings.weeklyWorkoutsGoal(),
        setSuggestion = settings.muscleGoal(),
        weightIncrease = settings.weightIncrease(),
        keepScreenOn = settings.keepScreenOn(),
        showFeedback = showFeedback,
        showDeveloper = showDeveloper,
        appInfo = config.appInfo(),
    )
}

// The weight increment in quarters (1.25 is 5), rounded onto them from a value saved off them and kept in range
internal fun weightIncreaseQuarters(value: Float): Int =
    Math.round(value * 4).coerceIn(SettingsTabLimits.WeightIncreaseQuarters)
