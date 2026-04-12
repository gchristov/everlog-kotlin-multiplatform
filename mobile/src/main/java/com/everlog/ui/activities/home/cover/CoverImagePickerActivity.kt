package com.everlog.ui.activities.home.cover

import android.graphics.Rect
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.everlog.R
import com.everlog.databinding.ActivityCoverImagePickerBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.views.recyclerview.infinitescroll.InfiniteRecyclerView
import com.everlog.utils.ViewUtils
import com.everlog.utils.device.DeviceUtils
import rx.Observable

class CoverImagePickerActivity : BaseActivity(), MvpViewCoverImagePicker {

    private var mPresenter: PresenterCoverImagePicker? = null
    private lateinit var binding: ActivityCoverImagePickerBinding

    override fun onActivityCreated() {
        setupTopBar()
        setupListView()
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_cover_image_picker
    }

    override fun getBindingView(): View? {
        binding = ActivityCoverImagePickerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_COVER_IMAGE_PICKER
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
        binding.root.findViewById<Toolbar>(R.id.toolbar).setNavigationIcon(R.drawable.ic_clear_white)
        setSupportActionBar(binding.root.findViewById(R.id.toolbar))
        supportActionBar?.title = "Choose Cover"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupListView() {
        binding.recyclerView.layoutManager = StaggeredGridLayoutManager(if (DeviceUtils.isTablet(this)) 3 else 2, StaggeredGridLayoutManager.VERTICAL)
        binding.recyclerView.adapter = mPresenter?.getListAdapter()
        binding.recyclerView.addItemDecoration(SpacesItemDecoration(ViewUtils.dpToPxFromRaw(this, R.dimen.margin_10)))
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