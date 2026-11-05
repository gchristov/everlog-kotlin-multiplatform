package com.everlog.android.statistics.ui.page

import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter

// The page's content is in StatisticsPageViewModel; this passes on when it shows and when settings or Remote Config
// change
class PresenterStatisticsPage(private val refresh: () -> Unit) : BaseFragmentPresenter<BaseFragmentMvpView>() {

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

    override fun onRemoteConfigChanged() {
        super.onRemoteConfigChanged()
        refresh()
    }
}
