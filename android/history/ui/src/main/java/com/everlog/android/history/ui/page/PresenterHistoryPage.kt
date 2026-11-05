package com.everlog.android.history.ui.page

import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter

// The page's content is in HistoryPageViewModel; this passes on when it shows and when settings change
class PresenterHistoryPage(private val refresh: () -> Unit) : BaseFragmentPresenter<BaseFragmentMvpView>() {

    override fun onReady() {
        // No-op
    }

    override fun onFragmentResumed() {
        super.onFragmentResumed()
        refresh()
    }

    override fun onPreferencesChanged() {
        super.onPreferencesChanged()
        refresh()
    }
}
