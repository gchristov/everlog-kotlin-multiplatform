package com.everlog.android.settings

import com.everlog.android.common.design.R as DesignR
import android.content.DialogInterface
import android.content.Intent
import com.everlog.android.settings.R
import com.everlog.android.auth.data.model.ELConsent
import com.everlog.android.auth.data.store.AuthStores
import com.everlog.android.auth.data.store.ELDocStoreUserLoadedEvent
import com.everlog.android.auth.data.store.ELUserConsentStore
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.mvp.MvpBroadcasts
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.settings.data.SettingsManager
import com.everlog.android.settings.data.model.ELIntegration
import com.everlog.android.settings.data.store.ELUserIntegrationStore
import com.everlog.android.settings.data.store.SettingsStores
import com.everlog.android.settings.integration.IntegrationActivity
import com.everlog.android.auth.data.LocalUserManager
import com.everlog.android.common.kotlin.format.ArrayResourceTypeUtils
import com.google.firebase.firestore.SetOptions
// import icepick.State
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.TextStyle
import java.util.*

class PresenterSettingsHome : BaseFragmentPresenter<MvpViewSettingsHome>() {

    private val dependencies = SettingsFeature.dependencies
    private val analytics = dependencies.settingsAnalytics()

    // @State
    @JvmField
    var mIntegrationGoogleFit: ELIntegration? = null
    // @State
    @JvmField
    var mConsent: ELConsent? = null

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
        // Integrations
        observeManageIntegrationGoogleFitClick()
        // Consent
        observeNewsletterCheckChange()
        loadData()
        loadIntegrationsData()
        loadConsentData()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        dependencies.googleFitActivityResult(requestCode, resultCode, data)
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onIntegrationLoaded(event: ELUserIntegrationStore.ELDocStoreIntegrationLoadedEvent) {
        if (isAttachedToView) {
            mIntegrationGoogleFit = if (event.error != null) null else { event.item }
            loadData()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConsentLoaded(event: ELUserConsentStore.ELDocStoreConsentLoadedEvent) {
        if (isAttachedToView) {
            mConsent = if (event.error != null) null else event.item
            loadData()
        }
    }

    // Observers

    private fun observeMuscleGoalClick() {
        subscriptions.add(mvpView.onClickMuscleGoal()
                .compose(applyUISchedulers())
                .subscribe({ context?.let(dependencies::openSetSuggestions) }, { throwable -> handleError(throwable) }))
    }

    private fun observeWeightIncreaseClick() {
        subscriptions.add(mvpView.onClickWeightIncrease()
                .compose(applyUISchedulers())
                .subscribe({ observeWeightIncreaseConfirm() }, { throwable -> handleError(throwable) }))
    }

    private fun observeWeightIncreaseConfirm() {
        subscriptions.add(mvpView.showPickerWeight(SettingsManager.manager.weightIncrease())
                .compose(applyUISchedulers())
                .subscribe({ value -> handleWeightIncreaseChanged(value) }, { throwable -> handleError(throwable) }))
    }

    private fun observeWeeklyGoalClick() {
        subscriptions.add(mvpView.onClickWeeklyGoal()
                .compose(applyUISchedulers())
                .subscribe({ observeWeeklyGoalConfirm() }, { throwable -> handleError(throwable) }))
    }

    private fun observeWeeklyGoalConfirm() {
        subscriptions.add(mvpView.showPickerWeeklyGoal(SettingsManager.manager.weeklyWorkoutsGoal())
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
                .subscribe({ context?.let(dependencies::openPlayStoreAppDetails) }, { throwable -> handleError(throwable) }))
    }

    private fun observeLogoutClick() {
        subscriptions.add(mvpView.onClickLogout()
                .compose(applyUISchedulers())
                .subscribe({ observeLogoutConfirm() }, { throwable -> handleError(throwable) }))
    }

    private fun observeLogoutConfirm() {
        subscriptions.add(mvpView.showDestructivePrompt(R.string.settings_logout, R.string.settings_logout_prompt, R.string.settings_logout, DesignR.string.cancel)
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
                .subscribe({ context?.let(dependencies::openSettingsFeedback) }, { throwable -> handleError(throwable) }))
    }

    private fun observeWeightUnitClick() {
        subscriptions.add(mvpView.onClickWeightUnit()
                .compose(applyUISchedulers())
                .subscribe({ observeWeightUnitConfirm() }, { throwable -> handleError(throwable) }))
    }

    private fun observeWeightUnitConfirm() {
        val titles = ArrayResourceTypeUtils.withWeightUnits().titles
        val selectedIndex = ArrayResourceTypeUtils.withWeightUnits().types.indexOf(SettingsManager.manager.weightUnit().name)
        subscriptions.add(mvpView.showPickerMultipleChoice(R.string.settings_unit_weight, titles, selectedIndex)
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
        val selectedIndex = collection.indexOf(SettingsManager.manager.firstDayOfWeek())
        subscriptions.add(mvpView.showPickerMultipleChoice(R.string.settings_first_week_day, titles.toTypedArray(), selectedIndex)
                .compose(applyUISchedulers())
                .subscribe({ value -> handleFirstWeekDayChanged(value) }, { throwable -> handleError(throwable) }))
    }

    private fun observeShareClick() {
        subscriptions.add(mvpView.onClickShare()
                .compose(applyUISchedulers())
                .subscribe({ context?.let(dependencies::shareApp) }, { throwable -> handleError(throwable) }))
    }

    private fun observeManageProClick() {
        subscriptions.add(mvpView.onClickManagePro()
                .compose(applyUISchedulers())
                .subscribe({ context?.let(dependencies::openProBuyOrManage) }, { throwable -> handleError(throwable) }))
    }

    private fun observeDesignSystemClick() {
        subscriptions.add(mvpView.onClickDesignSystem()
                .compose(applyUISchedulers())
                .subscribe({ context?.let(dependencies::openDesignSystem) }, { throwable -> handleError(throwable) }))
    }

    private fun observeManageIntegrationGoogleFitClick() {
        subscriptions.add(mvpView.onClickManageIntegrationGoogleFit()
                .compose(applyUISchedulers())
                .subscribe({
                    val integration = mIntegrationGoogleFit
                    if (integration != null) {
                        openIntegration(integration)
                    } else {
                        dependencies.connectGoogleFit(mvpView) { granted ->
                            mIntegrationGoogleFit = granted
                            openIntegration(granted)
                        }
                    }
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeNewsletterCheckChange() {
        subscriptions.add(mvpView.onCheckChangeNewsletter()
                .compose(applyUISchedulers())
                .subscribe({
                    saveConsentDecision(it)
                }, { throwable -> handleError(throwable) }))
    }


    private fun logout() {
        dependencies.logout {
            if (isAttachedToView) {
                context?.let(dependencies::openLogin)
            }
        }
    }

    // Loading

    private fun loadData() {
        mvpView?.showUserInfo(LocalUserManager.getUser()!!)
        mvpView?.showAppInfo(dependencies.appInfo())
        showFeedback()
        mvpView?.showSettings(
                SettingsHomeFragment.SettingsViewModel()
                        .muscleGoal(SettingsManager.manager.muscleGoal())
                        .weightIncrease(SettingsManager.manager.weightIncrease())
                        .weeklyGoals(SettingsManager.manager.weeklyWorkoutsGoal())
                        .weightUnit(SettingsManager.manager.weightUnit())
                        .keepScreenOn(SettingsManager.manager.keepScreenOn())
                        .firstWeekDay(SettingsManager.manager.firstDayOfWeek())
                        .integrationGoogleFitEnabled(mIntegrationGoogleFit != null)
                        .notificationNewsletterEnabled(mConsent?.getNewsletter())
        )
    }

    private fun showFeedback() {
        mvpView?.showFeedback(dependencies.hasSettingsFeedback())
    }

    private fun loadIntegrationsData() {
        // Listen for item changes
        SettingsStores.integrationStore().getItem(ELIntegration.Type.GOOGLE_FIT.name)
    }

    private fun loadConsentData() {
        // Listen for item changes
        AuthStores.consentStore().getItem(null)
    }

    // Consent


    private fun saveConsentDecision(granted: Boolean) {
        val consent = mConsent ?: ELConsent.newConsent(LocalUserManager.getUser()!!.id, granted)
        consent.updateNewsletter(granted)
        AuthStores.consentStore().create(consent, SetOptions.merge())
        if (granted) analytics.newsletterGranted() else analytics.newsletterDenied()
    }

    // Handlers

    private fun handleWeightIncreaseChanged(value: Float) {
        SettingsManager.manager.setWeightIncrease(value)
        loadData()
        analytics.weightIncreaseModified(value)
    }

    private fun handleWeeklyGoalChanged(value: Int) {
        SettingsManager.manager.setWeeklyWorkoutsGoal(value)
        loadData()
        notifyPreferencesChanged()
        analytics.weeklyGoalModified(value)
    }

    private fun handleKeepScreenOnChanged(value: Boolean) {
        SettingsManager.manager.setKeepScreenOn(value)
        loadData()
        analytics.keepScreenOnModified(value)
    }

    private fun handleWeightUnitChanged(index: Int) {
        val unit = WeightUnit.valueOf(ArrayResourceTypeUtils.withWeightUnits().types[index])
        SettingsManager.manager.setWeightUnit(unit)
        loadData()
        notifyPreferencesChanged()
        analytics.weightUnitModified(unit.name)
    }

    private fun handleFirstWeekDayChanged(index: Int) {
        val collection = arrayOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)
        val day = collection[index]
        SettingsManager.manager.setFirstDayOfWeek(day)
        loadData()
        notifyPreferencesChanged()
        analytics.firstWeekDayModified(day.name)
    }

    private fun openIntegration(integration: ELIntegration) {
        context?.let { it.startActivity(IntegrationActivity.launchIntent(it, IntegrationActivity.Companion.Properties().integration(integration))) }
    }

    private fun notifyPreferencesChanged() {
        sendBroadcast(Intent(MvpBroadcasts.PREFERENCES_CHANGED))
    }
}