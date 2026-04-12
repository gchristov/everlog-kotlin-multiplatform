package com.everlog.ui.fragments.home.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.everlog.R
import com.everlog.databinding.FragmentHomeActivityBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import com.everlog.ui.fragments.base.BaseFragmentPresenter
import com.everlog.ui.fragments.base.BaseTabFragment
import com.everlog.ui.fragments.home.activity.history.HistoryHomeFragment
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment

class ActivityHomeFragment : BaseTabFragment(), MvpViewActivityHome {

    private var mPresenter: PresenterActivityHome? = null
    private var _binding: FragmentHomeActivityBinding? = null
    private val binding get() = _binding!!

    override fun onFragmentCreated() {
        setupPager()
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_home_activity
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? {
        _binding = FragmentHomeActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_HOME_ACTIVITY
    }

    override fun getTitleResId(): Int {
        return -1
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        return mPresenter as? BaseFragmentPresenter<T>
    }

    fun showStatistics() {
//        binding.tabsPage.pager.setCurrentItem(0, true)
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterActivityHome()
    }

    private fun setupPager() {
        val fragments = ArrayList<BaseTabFragment>()
        fragments.add(StatisticsHomeFragment())
        fragments.add(HistoryHomeFragment())
        binding.tabsPage.setTabs(childFragmentManager, fragments)
    }
}