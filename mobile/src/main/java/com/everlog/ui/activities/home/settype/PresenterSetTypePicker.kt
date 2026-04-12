package com.everlog.ui.activities.home.settype

import android.app.Activity.RESULT_OK
import android.content.Intent
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.constants.ELConstants.EXTRA_SET_TYPE
import com.everlog.data.model.set.ELSetType
import com.everlog.data.model.util.Footer
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.adapters.FooterAdapter
import com.everlog.ui.adapters.SetTypeAdapter
import com.everlog.ui.navigator.Navigator
import com.everlog.utils.ArrayResourceTypeUtils

class PresenterSetTypePicker : BaseActivityPresenter<MvpViewSetTypePicker>() {

    private var mBinder: SetTypeAdapter.Binder? = null
    private val mAdapter = RecyclerAdapter()
    private val mDataListManager = DataListManager<Any>(mAdapter)

    init {
        setupListView()
    }

    override fun onReady() {
        mBinder?.setSelectedExercisesCount(mvpView.getSelectedExercisesCount())
        loadSetTypes()
    }

    internal fun getListAdapter(): RecyclerAdapter {
        return mAdapter
    }

    // Loading

    private fun loadSetTypes() {
        val items = ArrayList<Any>()
        for (typeString in ArrayResourceTypeUtils.withSetTypes().types) {
            items.add(ELSetType.valueOf(typeString))
        }
        val footer = Footer(R.string.select_set_type_footer, R.string.exercises_footer_link)
        items.add(footer)
        mDataListManager.addAll(items)
    }

    // Handlers

    private fun handleSetTypeSelected(type: ELSetType) {
        if (type.canBeSelected(mvpView.getSelectedExercisesCount())) {
            AnalyticsManager.manager.setTypeSelected(type)
            val i = Intent()
            i.putExtra(EXTRA_SET_TYPE, type)
            mvpView.setViewResult(RESULT_OK, i)
            mvpView.closeScreen()
        } else {
            mvpView.showToast(R.string.select_set_type_error_exercises)
        }
    }

    private fun handleFooterClick() {
        navigator.sendEmail(Navigator.ContactType.SETS, null)
    }

    // Setup

    private fun setupListView() {
        mAdapter.addDataManager(mDataListManager)
        mBinder = SetTypeAdapter.Binder { item, _ -> handleSetTypeSelected(item) }
        mAdapter.registerBinder(mBinder)
        mAdapter.registerBinder(FooterAdapter.Binder { _, _ -> handleFooterClick() })
    }
}