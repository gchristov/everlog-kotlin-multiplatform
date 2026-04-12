package com.everlog.ui.activities.home.exercise.details

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.everlog.R
import com.everlog.constants.ELConstants.EXTRA_EXERCISE
import com.everlog.constants.ELConstants.EXTRA_TYPE
import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.model.exercise.ELExercise
import com.everlog.databinding.ActivityExerciseDetailsBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.fragments.base.BaseTabFragment
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment
import com.everlog.ui.fragments.home.exercise.history.ExerciseHistoryFragment
import com.everlog.ui.fragments.home.exercise.info.ExerciseInfoFragment
import com.everlog.ui.fragments.home.exercise.statistics.ExerciseStatisticsFragment
import com.google.android.material.tabs.TabLayout
import rx.Observable
import rx.subjects.PublishSubject

class ExerciseDetailsActivity : BaseActivity(), MvpViewExerciseDetails {

    private var mPresenter: PresenterExerciseDetails? = null
    private lateinit var binding: ActivityExerciseDetailsBinding

    private var mAllowDelete = false
    private var mLastType: Type? = null

    private val mOnClickEdit = PublishSubject.create<Void>()
    private val mOnClickDelete = PublishSubject.create<Void>()

    companion object {

        enum class Type {
            INFO,
            STATISTICS,
            HISTORY,
        }

        class Properties {
            lateinit var exercise: ELExercise
                private set
            var type: Type = Type.INFO
                private set
            fun exercise(exercise: ELExercise) = apply { this.exercise = exercise }
            fun type(type: Type) = apply { this.type = type }
        }

        @JvmStatic
        fun launchIntent(context: Context, properties: Properties): Intent {
            val intent = Intent(context, ExerciseDetailsActivity::class.java)
            intent.putExtra(EXTRA_EXERCISE, properties.exercise)
            intent.putExtra(EXTRA_TYPE, properties.type)
            return intent
        }
    }

    override fun onActivityCreated() {
        setupTopBar()
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_EXERCISE_DETAILS
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (mAllowDelete) {
            menuInflater.inflate(R.menu.menu_activity_exercise_details, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_edit) {
            mOnClickEdit.onNext(null)
            return true
        } else if (id == R.id.action_delete) {
            mOnClickDelete.onNext(null)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_exercise_details
    }

    override fun getBindingView(): View? {
        binding = ActivityExerciseDetailsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun getExercise(): ELExercise {
        return intent.getSerializableExtra(EXTRA_EXERCISE) as ELExercise
    }

    override fun getType(): Type {
        return intent.getSerializableExtra(EXTRA_TYPE) as Type
    }

    override fun onClickEdit(): Observable<Void> {
        return mOnClickEdit
    }

    override fun onClickDelete(): Observable<Void> {
        return mOnClickDelete
    }

    override fun loadExerciseDetails(exercise: ELExercise,
                                     stats: ExerciseStatsController.StatsResult?,
                                     range: StatisticsHomeFragment.RangeType) {
        supportActionBar?.title = exercise.name
        supportActionBar?.subtitle = exercise.category?.lowercase()?.capitalize()
        mAllowDelete = exercise.isEditable()
        invalidateOptionsMenu()
        // Tabs
        val fragments = ArrayList<BaseTabFragment>()
        Type.values().forEach {
            when (it) {
                Type.INFO -> fragments.add(ExerciseInfoFragment(exercise, stats))
                Type.STATISTICS -> fragments.add(ExerciseStatisticsFragment(exercise, stats, range))
                Type.HISTORY -> fragments.add(ExerciseHistoryFragment(exercise, stats))
            }
        }
        if (mLastType == null) mLastType = getType()
        binding.tabsPage.setTabs(supportFragmentManager, fragments, Type.values().indexOf(mLastType), object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                mLastType = Type.values()[tab.position]
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        binding.tabsPage.enableNewBadge(1)
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterExerciseDetails()
    }

    private fun setupTopBar() {
        setSupportActionBar(binding.root.findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}