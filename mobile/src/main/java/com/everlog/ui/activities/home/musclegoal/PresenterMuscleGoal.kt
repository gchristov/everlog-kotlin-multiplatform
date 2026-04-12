package com.everlog.ui.activities.home.musclegoal

import android.content.Intent
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.constants.ELConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.adapters.MuscleGoalAdapter
import com.everlog.ui.navigator.Navigator
import com.everlog.utils.Utils

class PresenterMuscleGoal : BaseActivityPresenter<MvpViewMuscleGoal>() {

    private var mPendingMuscleGoal: SettingsManager.MuscleGoal? = null
    private val mAdapter = RecyclerAdapter()
    private val mDataListManager = DataListManager<Any>(mAdapter)

    init {
        setupListView()
    }

    override fun onReady() {
        observeFooterClick()
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
                .subscribe({ navigator.sendEmail(Navigator.ContactType.MUSCLE_GOAL, null) }, { throwable -> handleError(throwable) }))
    }

    // Loading

    private fun loadData() {
        val items = ArrayList<Any>()
        for (type in SettingsManager.MuscleGoal.availableGoals()) {
            items.add(type)
        }
        mDataListManager.clear()
        mDataListManager.addAll(items)
        mAdapter.notifyDataSetChanged()
        mvpView?.showData()
        // Check for pending Pro selection
        if (mPendingMuscleGoal?.proLocked() == true && userAccount?.isPro() == true) {
            setMuscleGoal(mPendingMuscleGoal!!, false)
        }
    }

    // Handlers

    private fun handleMuscleGoalSelected(muscleGoal: SettingsManager.MuscleGoal) {
        if (SettingsManager.manager.muscleGoal() != muscleGoal) {
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

    private fun setMuscleGoal(muscleGoal: SettingsManager.MuscleGoal, closeScreen: Boolean) {
        SettingsManager.manager.setMuscleGoal(muscleGoal)
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
        sendBroadcast(Intent(ELConstants.BROADCAST_PREFERENCES_CHANGED))
    }

    // Setup

    private fun setupListView() {
        mAdapter.addDataManager(mDataListManager)
        mAdapter.registerBinder(MuscleGoalAdapter.Binder { item, _ -> handleMuscleGoalSelected(item) })
    }
}