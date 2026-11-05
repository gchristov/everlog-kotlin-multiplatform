package com.everlog.android.home.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.common.mvp.fragment.BaseTabFragment
import com.everlog.android.history.ui.HistoryHomeFragment
import com.everlog.android.history.ui.page.HistoryPageFragment
import com.everlog.android.home.HomeGraph
import com.everlog.android.home.HomeScreenNames
import com.everlog.android.home.R
import com.everlog.android.home.databinding.FragmentHomeActivityBinding
import com.everlog.android.statistics.ui.StatisticsHomeFragment
import com.everlog.android.statistics.ui.page.StatisticsPageFragment

class ActivityHomeFragment : BaseTabFragment(), MvpViewActivityHome {

    private val graph by lazy { requireContext().appGraph<HomeGraph>() }
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
        return HomeScreenNames.ACTIVITY
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

    // Home moved to another tab: the history's pages can go. From the fragment manager, which holds the pages
    // restored after process death rather than the ones setupPager made.
    fun onTabLeft() {
        if (!isAdded) return
        childFragmentManager.fragments.filterIsInstance<HistoryPageFragment>().forEach { it.onTabLeft() }
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterActivityHome()
    }

    private fun setupPager() {
        val fragments = ArrayList<BaseTabFragment>()
        fragments.add(if (graph.homeConfig.statisticsV2()) StatisticsPageFragment() else StatisticsHomeFragment())
        fragments.add(if (graph.homeConfig.historyV2()) HistoryPageFragment() else HistoryHomeFragment())
        binding.tabsPage.setTabs(childFragmentManager, fragments)
    }
}