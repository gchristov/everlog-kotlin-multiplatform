package com.everlog.ui.views.base

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

abstract class BaseView : FrameLayout, BaseViewMvpView {

    protected abstract fun onViewCreated()

    protected abstract fun getLayoutResId(): Int

    protected abstract fun <T : BaseViewMvpView> getPresenter(): BaseViewPresenter<T>?

    protected abstract fun setupPresenter()

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        setupLayout(attrs, defStyle)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupPresenter()
        getPresenter<BaseViewMvpView>()?.attachView(this)
        getPresenter<BaseViewMvpView>()?.init()
        onViewCreated()
        getPresenter<BaseViewMvpView>()?.onReady()
    }

    override fun onDetachedFromWindow() {
        getPresenter<BaseViewMvpView>()?.detachView()
        super.onDetachedFromWindow()
    }

    // Setup

    open fun setupLayout(attrs: AttributeSet?, defStyleAttr: Int) {
        if (getLayoutResId() != 0) {
            inflate(context, getLayoutResId(), this)
        }
    }
}