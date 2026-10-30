package com.everlog.ui.activities.splash

import com.everlog.android.common.mvp.activity.BaseActivityMvpView

interface MvpViewSplash : BaseActivityMvpView {

    fun isLaunchedOverApp(): Boolean
}