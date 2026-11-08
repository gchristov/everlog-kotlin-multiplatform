package com.everlog.android.settings

import android.content.DialogInterface
import android.content.Intent
import com.everlog.android.auth.data.UserSession
import com.everlog.android.auth.data.requireUser
import com.everlog.android.auth.data.store.ELDocStoreUserLoadedEvent
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.kotlin.format.ArrayResourceTypeUtils
import com.everlog.android.common.mvp.MvpBroadcasts
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.common.navigation.DesignSystem
import com.everlog.android.common.navigation.FeedbackForm
import com.everlog.android.common.navigation.FeedbackSource
import com.everlog.android.common.navigation.Login
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.common.navigation.PlayStoreListing
import com.everlog.android.common.navigation.ProBuyOrManage
import com.everlog.android.common.navigation.SetSuggestions
import com.everlog.android.common.navigation.ShareApp
import com.everlog.android.settings.R
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.settings.ui.R as SettingsUiR
import com.everlog.android.settings.ui.SettingsAccount
import com.everlog.android.settings.ui.SettingsAnalytics
import com.everlog.android.settings.ui.SettingsConfig
// import icepick.State
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.TextStyle
import java.util.*

class PresenterSettingsHome(
    private val settings: UserSettings,
    private val session: UserSession,
    private val analytics: SettingsAnalytics,
    private val navigator: Navigator,
    private val config: SettingsConfig,
    private val account: SettingsAccount,
) : BaseFragmentPresenter<MvpViewSettingsHome>() {

    override fun onReady() {
        observeMuscleGoalClick()
        observeWeightIncreaseClick()
        observeWeeklyGoalClick()
        observeKeepScreenOnCheckChange()
        observeGooglePlayClick()
        observeLogoutClick()
        observeFeedbackClick()
        observeWeightUnitClick()
        observeFirstWeekDayClick()
        observeShareClick()
        observeManageProClick()
        observeDesignSystemClick()
        loadData()
    }

    override fun onPreferencesChanged() {
        super.onPreferencesChanged()
        loadData()
    }

    // The tab is built at launch, often before the first Remote Config refresh brings the form
    override fun onRemoteConfigChanged() {
        super.onRemoteConfigChanged()
        showFeedback()
    }

    override fun onProChanged() {
        super.onProChanged()
        loadData()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserLoaded(event: ELDocStoreUserLoadedEvent) {
        if (isAttachedToView) {
            loadData()
        }
    }

    // Observers

    private fun observeMuscleGoalClick() {
        subscriptions.add(mvpView.onClickMuscleGoal()
                .compose(applyUISchedulers())
                .subscribe({ context?.let { navigator.open(it, SetSuggestions) } }, { throwable -> handleError(throwable) }))
    }

    private fun observeWeightIncreaseClick() {
        subscriptions.add(mvpView.onClickWeightIncrease()
                .compose(applyUISchedulers())
                .subscribe({ observeWeightIncreaseConfirm() }, { throwable -> handleError(throwable) }))
    }

    private fun observeWeightIncreaseConfirm() {
        subscriptions.add(mvpView.showPickerWeight(settings.weightIncrease())
                .compose(applyUISchedulers())
                .subscribe({ value -> handleWeightIncreaseChanged(value) }, { throwable -> handleError(throwable) }))
    }

    private fun observeWeeklyGoalClick() {
        subscriptions.add(mvpView.onClickWeeklyGoal()
                .compose(applyUISchedulers())
                .subscribe({ observeWeeklyGoalConfirm() }, { throwable -> handleError(throwable) }))
    }

    private fun observeWeeklyGoalConfirm() {
        subscriptions.add(mvpView.showPickerWeeklyGoal(settings.weeklyWorkoutsGoal())
                .compose(applyUISchedulers())
                .subscribe({ value -> handleWeeklyGoalChanged(value) }, { throwable -> handleError(throwable) }))
    }

    private fun observeKeepScreenOnCheckChange() {
        subscriptions.add(mvpView.onCheckChangeKeepScreenOn()
                .compose(applyUISchedulers())
                .subscribe({ value -> handleKeepScreenOnChanged(value) }, { throwable -> handleError(throwable) }))
    }

    private fun observeGooglePlayClick() {
        subscriptions.add(mvpView.onClickGooglePlay()
                .compose(applyUISchedulers())
                .subscribe({ context?.let { navigator.open(it, PlayStoreListing) } }, { throwable -> handleError(throwable) }))
    }

    private fun observeLogoutClick() {
        subscriptions.add(mvpView.onClickLogout()
                .compose(applyUISchedulers())
                .subscribe({ observeLogoutConfirm() }, { throwable -> handleError(throwable) }))
    }

    private fun observeLogoutConfirm() {
        subscriptions.add(mvpView.showDestructivePrompt(SettingsUiR.string.settings_logout, SettingsUiR.string.settings_logout_prompt, SettingsUiR.string.settings_logout, DesignR.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        logout()
                    }
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeFeedbackClick() {
        subscriptions.add(mvpView.onClickFeedback()
                .compose(applyUISchedulers())
                .subscribe({ context?.let { navigator.open(it, FeedbackForm(FeedbackSource.SETTINGS)) } }, { throwable -> handleError(throwable) }))
    }

    private fun observeWeightUnitClick() {
        subscriptions.add(mvpView.onClickWeightUnit()
                .compose(applyUISchedulers())
                .subscribe({ observeWeightUnitConfirm() }, { throwable -> handleError(throwable) }))
    }

    private fun observeWeightUnitConfirm() {
        val titles = ArrayResourceTypeUtils.withWeightUnits().titles
        val selectedIndex = ArrayResourceTypeUtils.withWeightUnits().types.indexOf(settings.weightUnit().name)
        subscriptions.add(mvpView.showPickerMultipleChoice(SettingsUiR.string.settings_unit_weight, titles, selectedIndex)
                .compose(applyUISchedulers())
                .subscribe({ value -> handleWeightUnitChanged(value) }, { throwable -> handleError(throwable) }))
    }

    private fun observeFirstWeekDayClick() {
        subscriptions.add(mvpView.onClickFirstWeekDay()
                .compose(applyUISchedulers())
                .subscribe({ observeFirstWeekDayConfirm() }, { throwable -> handleError(throwable) }))
    }

    private fun observeFirstWeekDayConfirm() {
        val collection = arrayOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)
        val titles = collection.map { dayOfWeek -> dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
        val selectedIndex = collection.indexOf(settings.firstDayOfWeek())
        subscriptions.add(mvpView.showPickerMultipleChoice(SettingsUiR.string.settings_first_week_day, titles.toTypedArray(), selectedIndex)
                .compose(applyUISchedulers())
                .subscribe({ value -> handleFirstWeekDayChanged(value) }, { throwable -> handleError(throwable) }))
    }

    private fun observeShareClick() {
        subscriptions.add(mvpView.onClickShare()
                .compose(applyUISchedulers())
                .subscribe({ context?.let { navigator.open(it, ShareApp) } }, { throwable -> handleError(throwable) }))
    }

    private fun observeManageProClick() {
        subscriptions.add(mvpView.onClickManagePro()
                .compose(applyUISchedulers())
                .subscribe({ context?.let { navigator.open(it, ProBuyOrManage) } }, { throwable -> handleError(throwable) }))
    }

    private fun observeDesignSystemClick() {
        subscriptions.add(mvpView.onClickDesignSystem()
                .compose(applyUISchedulers())
                .subscribe({ context?.let { navigator.open(it, DesignSystem) } }, { throwable -> handleError(throwable) }))
    }

    private fun logout() {
        account.logout {
            if (isAttachedToView) {
                context?.let { navigator.open(it, Login) }
            }
        }
    }

    // Loading

    private fun loadData() {
        mvpView?.showUserInfo(session.requireUser())
        mvpView?.showAppInfo(config.appInfo())
        showFeedback()
        mvpView?.showSettings(
                SettingsHomeFragment.SettingsViewModel()
                        .muscleGoal(settings.muscleGoal())
                        .weightIncrease(settings.weightIncrease())
                        .weeklyGoals(settings.weeklyWorkoutsGoal())
                        .weightUnit(settings.weightUnit())
                        .keepScreenOn(settings.keepScreenOn())
                        .firstWeekDay(settings.firstDayOfWeek())
        )
    }

    private fun showFeedback() {
        mvpView?.showFeedback(navigator.canOpen(FeedbackForm(FeedbackSource.SETTINGS)))
    }

    // Handlers

    private fun handleWeightIncreaseChanged(value: Float) {
        settings.setWeightIncrease(value)
        loadData()
        analytics.weightIncreaseModified(value)
    }

    private fun handleWeeklyGoalChanged(value: Int) {
        settings.setWeeklyWorkoutsGoal(value)
        loadData()
        notifyPreferencesChanged()
        analytics.weeklyGoalModified(value)
    }

    private fun handleKeepScreenOnChanged(value: Boolean) {
        settings.setKeepScreenOn(value)
        loadData()
        analytics.keepScreenOnModified(value)
    }

    private fun handleWeightUnitChanged(index: Int) {
        val unit = WeightUnit.valueOf(ArrayResourceTypeUtils.withWeightUnits().types[index])
        settings.setWeightUnit(unit)
        loadData()
        notifyPreferencesChanged()
        analytics.weightUnitModified(unit.name)
    }

    private fun handleFirstWeekDayChanged(index: Int) {
        val collection = arrayOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)
        val day = collection[index]
        settings.setFirstDayOfWeek(day)
        loadData()
        notifyPreferencesChanged()
        analytics.firstWeekDayModified(day.name)
    }

    private fun notifyPreferencesChanged() {
        sendBroadcast(Intent(MvpBroadcasts.PREFERENCES_CHANGED))
    }
}