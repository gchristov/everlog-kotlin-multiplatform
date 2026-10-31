package com.everlog.android.template.feature.settype

import com.everlog.android.common.design.R as DesignR
import android.app.Activity.RESULT_OK
import android.content.Intent
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.mvp.activity.BaseActivityPresenter
import com.everlog.android.template.feature.R
import com.everlog.android.template.feature.TemplateExtras
import com.everlog.android.template.feature.TemplateFeature
import com.everlog.android.template.feature.settype.Footer
import com.everlog.android.template.feature.settype.FooterAdapter
import com.everlog.android.template.feature.settype.SetTypeAdapter
import com.everlog.android.common.kotlin.format.ArrayResourceTypeUtils

class PresenterSetTypePicker : BaseActivityPresenter<MvpViewSetTypePicker>() {

    private val feedback by lazy { mvpView.getActivity()?.let { TemplateFeature.dependencies.setTypesFeedback(it) } }

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
        if (feedback != null) {
            items.add(Footer(R.string.select_set_type_footer, DesignR.string.exercises_footer_link))
        }
        mDataListManager.addAll(items)
    }

    // Handlers

    private fun handleSetTypeSelected(type: ELSetType) {
        if (type.canBeSelected(mvpView.getSelectedExercisesCount())) {
            TemplateFeature.dependencies.templateAnalytics().setTypeSelected(type)
            val i = Intent()
            i.putExtra(TemplateExtras.SET_TYPE, type)
            mvpView.setViewResult(RESULT_OK, i)
            mvpView.closeScreen()
        } else {
            mvpView.showToast(R.string.select_set_type_error_exercises)
        }
    }

    private fun handleFooterClick() {
        feedback?.invoke()
    }

    // Setup

    private fun setupListView() {
        mAdapter.addDataManager(mDataListManager)
        mBinder = SetTypeAdapter.Binder { item, _ -> handleSetTypeSelected(item) }
        mAdapter.registerBinder(mBinder)
        mAdapter.registerBinder(FooterAdapter.Binder { _, _ -> handleFooterClick() })
    }
}