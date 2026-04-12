package com.everlog.ui.activities.home.musclegoal

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.everlog.R
import com.everlog.databinding.ActivityMuscleGoalBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.auth.LocalUserManager
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.views.notification.warning.WarningNotificationView
import com.jakewharton.rxbinding.view.RxView
import rx.Observable

class MuscleGoalActivity : BaseActivity(), MvpViewMuscleGoal {

    private var mPresenter: PresenterMuscleGoal? = null
    private lateinit var binding: ActivityMuscleGoalBinding

    override fun onActivityCreated() {
        setupTopBar()
        setupListView()
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_muscle_goal
    }

    override fun getBindingView(): View? {
        binding = ActivityMuscleGoalBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_MUSCLE_GOAL
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun onClickFooter(): Observable<Void> {
        return RxView.clicks(binding.footerView)
    }

    override fun showData() {
        binding.proUpgradePrompt.setType(WarningNotificationView.WarningType.PRO_MUSCLE_GOALS)
        binding.proUpgradePrompt.visibility = if (LocalUserManager.getUser()?.isPro() == true) View.GONE else View.VISIBLE
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterMuscleGoal()
    }

    private fun setupTopBar() {
        binding.appBar.toolbar.setNavigationIcon(R.drawable.ic_clear_white)
        setSupportActionBar(binding.appBar.toolbar)
        supportActionBar?.setTitle(R.string.muscle_goal_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupListView() {
        val mLayoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.adapter = mPresenter?.getListAdapter()
    }
}