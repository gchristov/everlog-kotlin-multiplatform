package com.everlog.ui.activities.home.musclegoal

import com.everlog.application.AppServices
import android.content.Intent
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.android.common.mvp.MvpBroadcasts
import com.everlog.android.settings.data.MuscleGoal
import com.everlog.config.FeedbackSource
import com.everlog.managers.RemoteConfigManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.android.auth.data.LocalUserManager
import com.everlog.android.common.mvp.activity.BaseActivityPresenter
import com.everlog.ui.adapters.MuscleGoalAdapter
import com.everlog.ui.navigator.navigator
import com.everlog.android.common.kotlin.Utils

class PresenterMuscleGoal : BaseActivityPresenter<MvpViewMuscleGoal>() {

    private var mPendingMuscleGoal: MuscleGoal? = null
    private val mAdapter = RecyclerAdapter()
    private val mDataListManager = DataListManager<Any>(mAdapter)

    init {
        setupListView()
    }

    override fun onReady() {
        observeFooterClick()
        mvpView.showFeedback(RemoteConfigManager.manager.feedbackFormUrl(FeedbackSource.MUSCLE_GOAL) != null)
        loadData()
    }

    override fun onProChanged() {
        super.onProChanged()
        loadData()
    }

    internal fun getListAdapter(): RecyclerAdapter {
        return mAdapter
    }

    // Observers

    private fun observeFooterClick() {
        subscriptions.add(mvpView.onClickFooter()
                .compose(applyUISchedulers())
                .subscribe({ navigator.openFeedbackForm(FeedbackSource.MUSCLE_GOAL) }, { throwable -> handleError(throwable) }))
    }

    // Loading

    private fun loadData() {
        val items = ArrayList<Any>()
        for (type in MuscleGoal.availableGoals()) {
            items.add(type)
        }
        mDataListManager.clear()
        mDataListManager.addAll(items)
        mAdapter.notifyDataSetChanged()
        mvpView?.showData()
        // Check for pending Pro selection
        if (mPendingMuscleGoal?.proLocked() == true && LocalUserManager.getUser()?.isPro() == true) {
            setMuscleGoal(mPendingMuscleGoal!!, false)
        }
    }

    // Handlers

    private fun handleMuscleGoalSelected(muscleGoal: MuscleGoal) {
        if (AppServices.userSettings.muscleGoal() != muscleGoal) {
            if (muscleGoal.proLocked()) {
                // Keep track of selected goal so it can be applied after user upgrades
                mPendingMuscleGoal = muscleGoal
                navigator.runProFeature {
                    setMuscleGoal(muscleGoal, true)
                }
            } else {
                setMuscleGoal(muscleGoal, true)
            }
        }
    }

    private fun setMuscleGoal(muscleGoal: MuscleGoal, closeScreen: Boolean) {
        AppServices.userSettings.setMuscleGoal(muscleGoal)
        notifyPreferencesChanged()
        AnalyticsManager.manager.settingsMuscleGoalModified(muscleGoal)
        mAdapter.notifyDataSetChanged()
        if (closeScreen) {
            Utils.runWithDelay({
                if (isAttachedToView) {
                    mvpView.closeScreen()
                }
            }, 200)
        }
    }

    private fun notifyPreferencesChanged() {
        sendBroadcast(Intent(MvpBroadcasts.PREFERENCES_CHANGED))
    }

    // Setup

    private fun setupListView() {
        mAdapter.addDataManager(mDataListManager)
        mAdapter.registerBinder(MuscleGoalAdapter.Binder { item, _ -> handleMuscleGoalSelected(item) })
    }
}