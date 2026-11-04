package com.everlog.android.home.weektab

import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter

// The tab's content is in WeekTabViewModel; this passes on when it shows and when settings or Remote Config change
class PresenterWeekTab(private val refresh: () -> Unit) : BaseFragmentPresenter<BaseFragmentMvpView>() {

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
