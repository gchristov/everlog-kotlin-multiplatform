package com.everlog.android.common.mvp.fragment

import android.content.Intent
import com.everlog.android.common.mvp.BaseBroadcastPresenter

abstract class BaseFragmentPresenter<T : BaseFragmentMvpView> : BaseBroadcastPresenter<T>() {

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // No-op
    }

    open fun onFragmentResumed() {
        // No-op
    }

    open fun onFragmentPaused() {
        // No-op
    }
}