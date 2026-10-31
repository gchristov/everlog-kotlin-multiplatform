package com.everlog.android.template.feature.details

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.everlog.android.template.feature.R
import com.everlog.android.template.feature.TemplateExtras
import com.everlog.android.template.feature.TemplateScreenNames
import com.everlog.android.template.feature.databinding.ActivityRoutineDetailsBinding
import com.everlog.android.template.feature.details.SummaryCardCollapsing
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.mvp.activity.BaseActivity
import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import com.everlog.android.common.mvp.activity.BaseActivityPresenter
import com.everlog.android.common.mvp.util.ELGlideModule
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.appbar.SubtitleCollapsingToolbarLayout
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import rx.subjects.PublishSubject
import com.everlog.android.common.design.R as DesignR
import kotlin.math.abs

open class RoutineDetailsActivity : BaseActivity(), MvpViewRoutineDetails {

    private var mPresenter: PresenterRoutineDetails<MvpViewRoutineDetails>? = null
    private lateinit var binding: ActivityRoutineDetailsBinding

    private var mViewOnly = false

    private val mOnClickEdit = PublishSubject.create<Void>()
    private val mOnClickDelete = PublishSubject.create<Void>()

    override fun onActivityCreated() {
        mViewOnly = intent?.extras?.getBoolean(TemplateExtras.VIEW_ONLY) ?: false
        setupInsets()
        setupTopBar()
        setupListView()
        setupButtons()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // The footer keeps itself above the navigation bar
            binding.recyclerView.setPadding(
                binding.recyclerView.paddingLeft,
                binding.recyclerView.paddingTop,
                binding.recyclerView.paddingRight,
                resources.getDimensionPixelSize(DesignR.dimen.footer_bottom_padding_double) + systemBars.bottom
            )

            insets
        }
    }

    override fun getRequiredExtras(): List<String> {
        return listOf(TemplateExtras.ROUTINE)
    }

    override fun getAnalyticsScreenName(): String {
        return TemplateScreenNames.DETAILS
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
        return intent.getSerializableExtra(TemplateExtras.ROUTINE) as ELRoutine
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
        binding.root.findViewById<Toolbar>(R.id.toolbar).setNavigationIcon(DesignR.drawable.ic_back)
        setSupportActionBar(binding.root.findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        ELGlideModule.loadImage(DesignR.drawable.background_workout, binding.root.findViewById(R.id.coverImage))
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