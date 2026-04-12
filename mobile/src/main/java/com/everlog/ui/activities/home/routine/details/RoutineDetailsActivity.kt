package com.everlog.ui.activities.home.routine.details

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.everlog.R
import com.everlog.constants.ELConstants.EXTRA_ROUTINE
import com.everlog.constants.ELConstants.EXTRA_VIEW_ONLY
import com.everlog.data.model.ELRoutine
import com.everlog.databinding.ActivityRoutineDetailsBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.views.summarycard.SummaryCardCollapsing
import com.everlog.utils.ActivityUtils
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.appbar.SubtitleCollapsingToolbarLayout
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import rx.subjects.PublishSubject
import kotlin.math.abs

open class RoutineDetailsActivity : BaseActivity(), MvpViewRoutineDetails {

    private var mPresenter: PresenterRoutineDetails<MvpViewRoutineDetails>? = null
    private lateinit var binding: ActivityRoutineDetailsBinding

    private var mViewOnly = false

    private val mOnClickEdit = PublishSubject.create<Void>()
    private val mOnClickDelete = PublishSubject.create<Void>()

    override fun onActivityCreated() {
        mViewOnly = intent?.extras?.getBoolean(EXTRA_VIEW_ONLY) ?: false
        setupTopBar()
        setupListView()
        setupButtons()
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_ROUTINE_DETAILS
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (!mViewOnly) {
            menuInflater.inflate(R.menu.menu_activity_routine_details, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_edit) {
            mOnClickEdit.onNext(null)
            return true
        } else if (item.itemId == R.id.action_delete) {
            mOnClickDelete.onNext(null)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_routine_details
    }

    override fun getBindingView(): View? {
        binding = ActivityRoutineDetailsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return getRoutinePresenter() as? BaseActivityPresenter<T>
    }

    protected open fun getRoutinePresenter(): PresenterRoutineDetails<*>? {
        return mPresenter
    }

    override fun onClickEdit(): Observable<Void> {
        return mOnClickEdit
    }

    override fun onClickDelete(): Observable<Void> {
        return mOnClickDelete
    }

    override fun onClickPerform(): Observable<Void> {
        return RxView.clicks(binding.performBtn)
    }

    override fun getItemToEdit(): ELRoutine {
        return intent.getSerializableExtra(EXTRA_ROUTINE) as ELRoutine
    }

    override fun loadItemDetails(routine: ELRoutine) {
        binding.root.findViewById<SubtitleCollapsingToolbarLayout>(R.id.toolbar_layout).title = routine.name
        binding.root.findViewById<TextView>(R.id.routineName).text = routine.name
        binding.root.findViewById<SummaryCardCollapsing>(R.id.exercisesSummary).setSummary(routine.getTotalExercises().toString(), "Exercises")
        binding.root.findViewById<SummaryCardCollapsing>(R.id.setsSummary).setSummary(routine.getTotalSets().toString(), "Sets")
        val restTimeSeconds = routine.getRestTimeSeconds()
        if (routine.getRestTimeSeconds() > 0) {
            binding.root.findViewById<SummaryCardCollapsing>(R.id.restSummary).setSummary(restTimeSeconds.toString(), "Rest (s)")
        } else {
            binding.root.findViewById<SummaryCardCollapsing>(R.id.restSummary).setSummary("--", "No rest")
        }
    }

    override fun toggleEmptyViewVisible(visible: Boolean) {
        binding.recyclerView.visibility = if (visible) View.GONE else View.VISIBLE
        binding.emptyView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterRoutineDetails()
    }

    private fun setupTopBar() {
        binding.root.findViewById<Toolbar>(R.id.toolbar).setNavigationIcon(R.drawable.ic_back)
        setSupportActionBar(binding.root.findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ActivityUtils.setupWorkoutCoverImage(binding.root.findViewById(R.id.coverImage))
        binding.root.findViewById<AppBarLayout>(R.id.appBar).addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout: AppBarLayout, verticalOffset: Int ->
            val percentage = abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange
            binding.root.findViewById<View>(R.id.routineInfoContainer).alpha = 1 - percentage
        })
    }

    private fun setupListView() {
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = getRoutinePresenter()?.getListAdapter()
    }

    private fun setupButtons() {
        binding.performBtn.visibility = if (!mViewOnly) View.VISIBLE else View.GONE
    }
}