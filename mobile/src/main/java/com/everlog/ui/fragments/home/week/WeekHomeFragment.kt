package com.everlog.ui.fragments.home.week

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.everlog.R
import com.everlog.config.HomeNotification
import com.everlog.data.controllers.statistics.UserStatsController
import com.everlog.data.model.plan.ELPlan
import com.everlog.data.model.plan.ELPlanState
import com.everlog.databinding.FragmentHomeWeekBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.home.HomeActivity
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import com.everlog.ui.fragments.base.BaseFragmentPresenter
import com.everlog.ui.fragments.base.BaseTabFragment
import com.everlog.ui.fragments.home.week.views.PlanWeekView
import com.everlog.ui.fragments.home.week.views.PlanWeekView.PlanWeekListener
import com.everlog.ui.fragments.home.week.views.StatisticsWeekView
import com.everlog.ui.fragments.home.week.views.StatisticsWeekView.StatisticsWeekListener
import rx.Observable
import rx.subjects.PublishSubject

class WeekHomeFragment : BaseTabFragment(), MvpViewWeekHome {

    private val mWeekStatistics = StatisticsWeekView()
    private val mWeekPlan = PlanWeekView()
    private val mWeekViews = arrayOf(mWeekStatistics, mWeekPlan)

    private var mPresenter: PresenterWeekHome? = null
    private var _binding: FragmentHomeWeekBinding? = null
    private val binding get() = _binding!!

    private val mPlanClick = PublishSubject.create<Void>()
    private val mPlanStartClick = PublishSubject.create<Void>()
    private val mPlanSkipClick = PublishSubject.create<Void>()
    private val mWeekStatsClick = PublishSubject.create<Void>()
    private val mWeekGoalClick = PublishSubject.create<Void>()
    private val mWeekEmptyStateClick = PublishSubject.create<Void>()

    override fun onFragmentCreated() {
        setupWeekViews()
        checkAppRate()
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_HOME_WEEK
    }

    override fun getTitleResId(): Int {
        return -1
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_home_week
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? {
        _binding = FragmentHomeWeekBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        return mPresenter as? BaseFragmentPresenter<T>
    }

    override fun onClickWeekStats(): Observable<Void> {
        return mWeekStatsClick
    }

    override fun onClickWeekGoal(): Observable<Void> {
        return mWeekGoalClick
    }

    override fun onClickWeekEmptyState(): Observable<Void> {
        return mWeekEmptyStateClick
    }

    override fun onClickPlan(): Observable<Void> {
        return mPlanClick
    }

    override fun onClickPlanSkip(): Observable<Void> {
        return mPlanSkipClick
    }

    override fun onClickPlanStart(): Observable<Void> {
        return mPlanStartClick
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        mWeekViews.forEach { it.toggleLoading(show, activity as BaseActivity) }
    }

    override fun showWeekData(stats: UserStatsController.StatsResult?) {
        mWeekViews.forEach { it.toggleVisible(false) }
        mWeekStatistics.toggleVisible(true)
        mWeekStatistics.showWeekData(stats)
        checkAppRate()
        binding.toolbar.title = mWeekStatistics.title
    }

    override fun showWeekData(plan: ELPlan?, state: ELPlanState?) {
        mWeekViews.forEach { it.toggleVisible(false) }
        mWeekPlan.toggleVisible(true)
        mWeekPlan.showWeekData(plan, state)
        binding.toolbar.title = mWeekPlan.title
    }

    override fun showHomeNotification(notification: HomeNotification?) {
        binding.notificationView.showHomeNotification(notification)
    }

    override fun showStatistics() {
        (activity as? HomeActivity)?.showStatistics()
    }

    override fun showSettings() {
        (activity as? HomeActivity)?.showSettings()
    }

    override fun showCreateActivity() {
        (activity as? HomeActivity)?.showCreateActivity()
    }

    private fun checkAppRate() {
        binding.appRateView.checkAppRate()
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterWeekHome()
    }

    private fun setupWeekViews() {
        mWeekViews.forEach {
            it.onCreateView(view)
            it.toggleVisible(false)
        }
        mWeekStatistics.setListener(object : StatisticsWeekListener {

            override fun onClickStats() {
                mWeekStatsClick.onNext(null)
            }

            override fun onClickGoal() {
                mWeekGoalClick.onNext(null)
            }

            override fun onClickEmptyState() {
                mWeekEmptyStateClick.onNext(null)
            }
        })
        mWeekPlan.setListener(object : PlanWeekListener {

            override fun onClickStart() {
                mPlanStartClick.onNext(null)
            }

            override fun onClickPlan() {
                mPlanClick.onNext(null)
            }

            override fun onClickSkip() {
                mPlanSkipClick.onNext(null)
            }
        })
    }
}