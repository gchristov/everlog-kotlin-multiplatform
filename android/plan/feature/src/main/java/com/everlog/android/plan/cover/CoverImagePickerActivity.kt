package com.everlog.android.plan.cover

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.everlog.android.common.mvp.activity.BaseActivity
import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import com.everlog.android.common.mvp.activity.BaseActivityPresenter
import com.everlog.android.common.mvp.util.ViewUtils
import com.everlog.android.plan.PlanScreenNames
import com.everlog.android.plan.R
import com.everlog.android.plan.cover.InfiniteRecyclerView
import com.everlog.android.plan.databinding.ActivityCoverImagePickerBinding
import rx.Observable
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.mvp.R as MvpR

class CoverImagePickerActivity : BaseActivity(), MvpViewCoverImagePicker {

    companion object {
        // Hands back the picked image's URL in PlanExtras.COVER_IMAGE
        @JvmStatic
        fun launchIntent(context: Context): Intent = Intent(context, CoverImagePickerActivity::class.java)
    }

    private var mPresenter: PresenterCoverImagePicker? = null
    private lateinit var binding: ActivityCoverImagePickerBinding

    override fun onActivityCreated() {
        setupTopBar()
        setupListView()
        setupInsets()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Top inset for AppBar
            val toolbar = binding.root.findViewById<Toolbar>(MvpR.id.toolbar)
            val appBar = toolbar.parent as? View
            appBar?.updatePadding(top = systemBars.top)
            
            // Bottom inset for RecyclerView
            binding.recyclerView.updatePadding(bottom = systemBars.bottom + ViewUtils.dpToPxFromRaw(this, DesignR.dimen.margin_10))
            
            insets
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_cover_image_picker
    }

    override fun getBindingView(): View? {
        binding = ActivityCoverImagePickerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAnalyticsScreenName(): String {
        return PlanScreenNames.COVER_IMAGE_PICKER
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun onClickEmptyAction(): Observable<Void> {
        return binding.emptyView.onActionClick()
    }

    override fun onLoadMore(): Observable<Void> {
        return binding.recyclerView.observeLoadMore()
    }

    override fun getInfiniteList(): InfiniteRecyclerView {
        return binding.recyclerView
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        toggleShimmerLayout(binding.root.findViewById(R.id.shimmerView), show, true)
    }

    override fun toggleEmptyState(visible: Boolean) {
        binding.recyclerView.visibility = if (visible) View.GONE else View.VISIBLE
        binding.emptyView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterCoverImagePicker()
    }

    private fun setupTopBar() {
        binding.root.findViewById<Toolbar>(MvpR.id.toolbar).setNavigationIcon(DesignR.drawable.ic_clear_white)
        setSupportActionBar(binding.root.findViewById(MvpR.id.toolbar))
        supportActionBar?.title = "Choose Cover"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupListView() {
        binding.recyclerView.layoutManager = StaggeredGridLayoutManager(if (resources.getBoolean(DesignR.bool.isTablet)) 3 else 2, StaggeredGridLayoutManager.VERTICAL)
        binding.recyclerView.adapter = mPresenter?.getListAdapter()
        binding.recyclerView.addItemDecoration(SpacesItemDecoration(ViewUtils.dpToPxFromRaw(this, DesignR.dimen.margin_10)))
    }

    class SpacesItemDecoration(private val mSpace: Int) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.left = mSpace
            outRect.right = mSpace
            outRect.bottom = mSpace
            outRect.top = mSpace
        }
    }
}