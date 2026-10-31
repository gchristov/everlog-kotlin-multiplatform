package com.everlog.android.plan.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.databinding.FragmentPlanWeekBinding
import com.everlog.android.common.mvp.fragment.BaseFragment
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.plan.R

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