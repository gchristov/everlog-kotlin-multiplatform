package com.everlog.android.home.workoutstab

import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter

// The tab's content is in WorkoutsTabViewModel; this passes on when it shows and stops showing
class PresenterWorkoutsTab(
    private val onShown: () -> Unit,
    private val onHidden: () -> Unit,
) : BaseFragmentPresenter<BaseFragmentMvpView>() {

    override fun onReady() {
        // No-op
    }

    override fun onFragmentResumed() {
        super.onFragmentResumed()
        onShown()
    }

    override fun onFragmentPaused() {
        super.onFragmentPaused()
        onHidden()
    }
}
