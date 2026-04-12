package com.everlog.ui.activities.home.exercise

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.databinding.ActivityExercisesBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.activities.home.search.BaseSearchActivity
import com.everlog.ui.dialog.DialogBuilder
import com.everlog.ui.onboarding.ExercisesOnboardingController
import com.everlog.ui.views.revealcircle.FilterExercisesView
import com.facebook.shimmer.ShimmerFrameLayout
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import rx.subjects.PublishSubject

class ExercisesActivity : BaseSearchActivity(), MvpViewExercises {

    private var mFilterView: FilterExercisesView? = null
    private var menu: Menu? = null
    private lateinit var binding: ActivityExercisesBinding

    private var mPresenter: PresenterExercises? = null
    private var mOnboardingController: ExercisesOnboardingController? = null

    private val mOnFiltersClick = PublishSubject.create<Void>()
    private val mOnFiltersChanged = PublishSubject.create<FilterExercisesView.ExerciseFilters>()
    private val mOnAddClick = PublishSubject.create<Void>()

    companion object {

        class Properties {
            var selection: Boolean = false
                private set
            fun selection(selection: Boolean) = apply { this.selection = selection }
        }

        @JvmStatic
        fun launchIntent(context: Context, properties: Properties): Intent {
            val intent = Intent(context, ExercisesActivity::class.java)
            intent.putExtra(ELConstants.EXTRA_SELECTION, properties.selection)
            return intent
        }
    }

    override fun onActivityCreated() {
        super.onActivityCreated()
        setupTopBar()
        setupListView()
        setupOnboarding()
    }

    override fun getSearchToolbar(): Toolbar {
        return binding.root.findViewById(R.id.searchToolbar)
    }

    override fun onBackPressed() {
        if (mFilterView != null && mFilterView?.isAttachedToWindow == true) {
            mOnFiltersClick.onNext(null)
        } else {
            super.onBackPressed()
        }
    }

    override fun getAnalyticsScreenName(): String {
        return if (isSelectionMode()) AnalyticsConstants.SCREEN_EXERCISE_PICKER else AnalyticsConstants.SCREEN_EXERCISES
    }

    override fun getLayoutResId(): Int {
        return 0
    }

    override fun getBindingView(): View? {
        binding = ActivityExercisesBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_activity_exercises, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                showSearch()
                true
            }
            R.id.action_filter -> {
                mOnFiltersClick.onNext(null)
                true
            }
            R.id.action_create -> {
                mOnAddClick.onNext(null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSearchChanged(): Observable<String> {
        return mOnSearchChanged
    }

    override fun onClickAdd(): Observable<Void> {
        return mOnAddClick
    }

    override fun onSearchHidden(): Observable<Void> {
        return mOnSearchHidden
    }

    override fun onClickEmptyAction(): Observable<Void> {
        return binding.emptyView.onActionClick()
    }

    override fun onClickSelectionLink(): Observable<Void> {
        return RxView.clicks(binding.linkBtn)
    }

    override fun onClickSelectionAdd(): Observable<Void> {
        return RxView.clicks(binding.addBtn)
    }

    override fun onFiltersClick(): Observable<Void> {
        return mOnFiltersClick
    }

    override fun onFiltersChanged(): Observable<FilterExercisesView.ExerciseFilters> {
        return mOnFiltersChanged
    }

    override fun isSelectionMode(): Boolean {
        return intent.getBooleanExtra(ELConstants.EXTRA_SELECTION, false)
    }

    override fun getScrollPosition(): Int {
        return (binding.recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    }

    override fun getOnboardingController(): ExercisesOnboardingController? {
        return mOnboardingController
    }

    override fun setScrollPosition(position: Int) {
        binding.recyclerView.layoutManager?.scrollToPosition(position)
    }

    override fun stopSearch() {
        super.stopSearch()
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        toggleShimmerLayout(binding.root.findViewById(R.id.shimmerView), show, true)
        if (show) {
            binding.emptyView.visibility = View.GONE
        }
    }

    override fun toggleEmptyState(visible: Boolean, searchText: String?) {
        binding.emptyView.visibility = if (visible) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (visible) View.GONE else View.VISIBLE
        if (searchText != null) {
            binding.emptyView.setImage(-1)
            binding.emptyView.setTitle(getString(if (searchText.isEmpty()) R.string.exercises_empty_title else R.string.exercises_empty_search, searchText))
            binding.emptyView.setSubtitle(-1)
        } else {
            binding.emptyView.setImage(R.drawable.ic_barbell)
            binding.emptyView.setTitle(R.string.exercises_empty_title)
            binding.emptyView.setSubtitle(R.string.exercises_empty_subtitle)
        }
    }

    override fun toggleAddBtn(visible: Boolean, selectionCount: Int) {
        binding.addBtnContainer.visibility = if (visible) View.VISIBLE else View.GONE
        binding.linkBtn.text = getString(R.string.exercises_link, selectionCount)
        binding.addBtn.text = getString(R.string.exercises_add, selectionCount)
    }

    override fun toggleFilters(filters: FilterExercisesView.ExerciseFilters) {
        val menuItem = menu?.getItem(0)
        menuItem?.icon = ContextCompat.getDrawable(this, if (filters.hasFilter()) R.drawable.ic_filter_on else R.drawable.ic_filter)
        if (mFilterView == null || mFilterView?.isAttachedToWindow == false) {
            mFilterView = FilterExercisesView(this, filters, object : FilterExercisesView.OnFilterListener {

                override fun onSave(filter: FilterExercisesView.ExerciseFilters) {
                    mOnFiltersChanged.onNext(filter)
                }
            })
            val targetView = getFiltersMenu() ?: binding.containerLayout
            val location = IntArray(2)
            targetView.getLocationInWindow(location)
            mFilterView?.show(binding.containerLayout, Point(location[0], location[1]))
        } else {
            mFilterView?.hide(binding.containerLayout)
            mFilterView = null
        }
    }

    override fun scrollToTop() {
        binding.recyclerView.layoutManager?.scrollToPosition(0)
    }

    override fun showPickerMultipleChoice(values: Array<String>, selectedIndex: Int, type: DialogBuilder.MultipleChoiceDialogType): Observable<Int> {
        return DialogBuilder.showPickerMultipleChoiceDialog(this, values, selectedIndex, type)
    }

    fun getFiltersMenu(): View? {
        return findViewById(R.id.action_filter)
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterExercises()
    }

    private fun setupTopBar() {
        setSupportActionBar(binding.root.findViewById(R.id.toolbar))
        if (isSelectionMode()) {
            supportActionBar?.setTitle(R.string.exercises_title_select)
            binding.root.findViewById<Toolbar>(R.id.toolbar).setNavigationIcon(R.drawable.ic_clear_white)
        } else {
            supportActionBar?.setTitle(R.string.exercises_title)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupListView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = mPresenter?.getListAdapter()
    }

    private fun setupOnboarding() {
        mOnboardingController = ExercisesOnboardingController(this)
    }
}