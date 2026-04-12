package com.everlog.ui.activities.home.settype

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.databinding.ActivitySetTypePickerBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter

class SetTypePickerActivity : BaseActivity(), MvpViewSetTypePicker {

    private var mPresenter: PresenterSetTypePicker? = null
    private lateinit var binding: ActivitySetTypePickerBinding

    override fun onActivityCreated() {
        setupTopBar()
        setupListView()
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_set_type_picker
    }

    override fun getBindingView(): View? {
        binding = ActivitySetTypePickerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_SET_TYPE_PICKER
    }

    override fun getSelectedExercisesCount(): Int {
        return intent?.getIntExtra(ELConstants.EXTRA_NUMBER_OF_ITEMS, 1) ?: 1
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterSetTypePicker()
    }

    private fun setupTopBar() {
        binding.root.findViewById<Toolbar>(R.id.toolbar).setNavigationIcon(R.drawable.ic_clear_white)
        setSupportActionBar(binding.root.findViewById(R.id.toolbar))
        supportActionBar?.setTitle(R.string.select_set_type)
        supportActionBar?.subtitle = resources.getQuantityString(R.plurals.select_set_type_subtitle, getSelectedExercisesCount(), getSelectedExercisesCount())
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupListView() {
        val mLayoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.adapter = mPresenter?.getListAdapter()
    }
}