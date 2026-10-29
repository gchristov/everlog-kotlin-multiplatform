package com.everlog.ui.bottomsheets.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.everlog.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import timber.log.Timber

abstract class BaseBottomSheet : BottomSheetDialogFragment(), BaseBottomSheetMvpView {

    abstract fun getLogTag(): String

    abstract fun onSheetCreated()

    abstract fun getLayoutResId(): Int

    open fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? = null

    abstract fun <T : BaseBottomSheetMvpView> getPresenter(): BaseBottomSheetPresenter<T>?

    abstract fun setupPresenter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val bindingView = getBindingView(inflater, container)
        val contentView = if (bindingView != null) {
            bindingView
        } else {
            val view = inflater.inflate(getLayoutResId(), container, false)
            view
        }

        setupPresenter()
        getPresenter<BaseBottomSheetMvpView>()?.onRestoreInstanceState(savedInstanceState)
        getPresenter<BaseBottomSheetMvpView>()?.attachView(this)
        getPresenter<BaseBottomSheetMvpView>()?.init()
        onSheetCreated()
        getPresenter<BaseBottomSheetMvpView>()?.onReady()
        return contentView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        getPresenter<BaseBottomSheetMvpView>()?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.tag(getLogTag()).e(e)
        }
    }
}