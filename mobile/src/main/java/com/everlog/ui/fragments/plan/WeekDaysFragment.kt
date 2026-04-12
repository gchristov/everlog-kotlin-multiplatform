package com.everlog.ui.fragments.plan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.everlog.R
import com.everlog.data.model.plan.ELPlan
import com.everlog.databinding.FragmentPlanWeekBinding
import com.everlog.ui.fragments.base.BaseFragment
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import com.everlog.ui.fragments.base.BaseFragmentPresenter

class WeekDaysFragment : BaseFragment(), MvpViewWeekDays {

    private var mPresenter: PresenterWeekDays? = null
    private var _binding: FragmentPlanWeekBinding? = null
    private val binding get() = _binding!!

    private var mPlan: ELPlan? = null
    private var mWeekIndex = 0

    override fun onFragmentCreated() {
        setupListView()
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_plan_week
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? {
        _binding = FragmentPlanWeekBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        return mPresenter as? BaseFragmentPresenter<T>
    }

    override fun getPlan(): ELPlan? {
        return mPlan
    }

    override fun getWeekIndex(): Int {
        return mWeekIndex
    }

    fun setData(plan: ELPlan?, weekIndex: Int) {
        this.mPlan = plan
        this.mWeekIndex = weekIndex
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterWeekDays()
    }

    private fun setupListView() {
        val mLayoutManager = LinearLayoutManager(getParentActivity())
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.adapter = mPresenter?.getListAdapter()
    }
}