package com.everlog.ui.fragments.base

import android.content.Intent
import com.everlog.ui.mvp.BaseBroadcastPresenter

abstract class BaseFragmentPresenter<T : BaseFragmentMvpView> : BaseBroadcastPresenter<T>() {

    internal open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // No-op
    }

    internal open fun onFragmentResumed() {
        // No-op
    }

    internal open fun onFragmentPaused() {
        // No-op
    }
}