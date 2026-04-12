package com.everlog.ui.activities.home.routine

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.everlog.R
import com.everlog.databinding.ActivityRoutinePickerBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.facebook.shimmer.ShimmerFrameLayout
import com.jakewharton.rxbinding.view.RxView
import rx.Observable

class RoutinePickerActivity : BaseActivity(), MvpViewRoutinePicker {

    private var mPresenter: PresenterRoutinePicker? = null
    private lateinit var binding: ActivityRoutinePickerBinding

    public override fun onActivityCreated() {
        setupTopBar()
        setupListView()
    }

    public override fun getLayoutResId(): Int {
        return R.layout.activity_routine_picker
    }

    override fun getBindingView(): View? {
        binding = ActivityRoutinePickerBinding.inflate(layoutInflater)
        return binding.root
    }

    public override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_ROUTINE_PICKER
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun onClickAddRoutine(): Observable<Void> {
        return RxView.clicks(binding.addBtn)
    }

    override fun onClickEmptyAction(): Observable<Void> {
        return binding.emptyView.onActionClick()
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        toggleShimmerLayout(binding.root.findViewById(R.id.shimmerView), show, true)
    }

    override fun toggleEmptyState(visible: Boolean) {
        binding.recyclerView.visibility = if (visible) View.GONE else View.VISIBLE
        binding.emptyView.visibility = if (visible) View.VISIBLE else View.GONE
        if (visible) {
            binding.addBtn.visibility = View.GONE
        } else {
            binding.addBtn.visibility = View.VISIBLE
        }
    }

    // Setup

    public override fun setupPresenter() {
        mPresenter = PresenterRoutinePicker()
    }

    private fun setupTopBar() {
        setSupportActionBar(binding.root.findViewById(R.id.toolbar))
        supportActionBar?.setTitle(R.string.routine_picker_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.root.findViewById<Toolbar>(R.id.toolbar).setNavigationIcon(R.drawable.ic_clear_white)
    }

    private fun setupListView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = mPresenter?.getListAdapter()
    }
}