package com.everlog.ui.activities.pending

import android.view.View
import com.everlog.R
import com.everlog.databinding.ActivityPendingFeatureBinding
import com.everlog.ui.activities.base.BaseActivity
import rx.Observable

abstract class PendingFeatureActivity : BaseActivity(), MvpViewPendingFeature {

    private lateinit var binding: ActivityPendingFeatureBinding

    override fun onActivityCreated() {
        // No-op.
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_pending_feature
    }

    override fun getBindingView(): View? {
        binding = ActivityPendingFeatureBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onClickEmptyAction(): Observable<Void> {
        return binding.emptyView.onActionClick()
    }
}