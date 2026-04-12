package com.everlog.ui.fragments.home.workouts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.everlog.R
import com.everlog.databinding.FragmentHomeWorkoutsBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import com.everlog.ui.fragments.base.BaseFragmentPresenter
import com.everlog.ui.fragments.base.BaseTabFragment
import com.jakewharton.rxbinding.view.RxView
import rx.Observable

class WorkoutsHomeFragment : BaseTabFragment(), MvpViewWorkoutsHome {

    private var mPresenter: PresenterWorkoutsHome? = null
    private var _binding: FragmentHomeWorkoutsBinding? = null
    private val binding get() = _binding!!

    override fun onFragmentCreated() {
        setupListView()
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_HOME_WORKOUTS
    }

    override fun getTitleResId(): Int {
        return -1
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_home_workouts
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? {
        _binding = FragmentHomeWorkoutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        return mPresenter as? BaseFragmentPresenter<T>
    }

    override fun onClickExercises(): Observable<Void> {
        return RxView.clicks(binding.exercisesBtn)
    }

    override fun onClickAddPlanTop(): Observable<Void> {
        return RxView.clicks(binding.addPlanTop.root)
    }

    override fun togglePlanCreateTop(visible: Boolean) {
        binding.addPlanTop.root.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    override fun toggleLoadingOverlayPlans(show: Boolean) {
        BaseActivity.toggleShimmerLayout(view?.findViewById(R.id.shimmerViewPlans), show, true)
        binding.recyclerViewPlans.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onClickAddRoutineTop(): Observable<Void> {
        return RxView.clicks(binding.addRoutineTop.root)
    }

    override fun toggleRoutineCreateTop(visible: Boolean) {
        binding.addRoutineTop.root.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    override fun toggleLoadingOverlayRoutines(show: Boolean) {
        BaseActivity.toggleShimmerLayout(view?.findViewById(R.id.shimmerViewRoutines), show, true)
        binding.recyclerViewRoutines.visibility = if (show) View.GONE else View.VISIBLE
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterWorkoutsHome()
    }

    private fun setupListView() {
        // Plans
        var layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewPlans.layoutManager = layoutManager
        binding.recyclerViewPlans.adapter = mPresenter?.getPlansAdapter()
        // Routines
        layoutManager = LinearLayoutManager(context)
        binding.recyclerViewRoutines.layoutManager = layoutManager
        binding.recyclerViewRoutines.adapter = mPresenter?.getRoutinesAdapter()
    }
}