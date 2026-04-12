package com.everlog.ui.views.base

import com.everlog.ui.mvp.BasePresenter

abstract class BaseViewPresenter<T : BaseViewMvpView> : BasePresenter<T>()