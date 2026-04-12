package com.everlog.ui.activities.home.plan.details

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import com.everlog.R
import com.everlog.constants.ELConstants.EXTRA_PLAN_UUID
import com.everlog.data.model.plan.ELPlan
import com.everlog.databinding.ActivityPlanDetailsBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.fragments.plan.WeekDaysFragment
import com.everlog.ui.views.viewpager.ELFragmentPagerAdapter
import com.everlog.utils.glide.ELGlideModule
import com.google.android.material.appbar.SubtitleCollapsingToolbarLayout
import com.jakewharton.rxbinding.view.RxView
import ru.tinkoff.scrollingpagerindicator.ScrollingPagerIndicator
import rx.Observable
import rx.subjects.PublishSubject

class PlanDetailsActivity : BaseActivity(), MvpViewPlanDetails {

    private var mPresenter: PresenterPlanDetails? = null
    private lateinit var binding: ActivityPlanDetailsBinding

    private var mAdapter: WeekPagerAdapter? = null
    private var mAllowDelete = false

    private val mOnClickEdit = PublishSubject.create<Void>()
    private val mOnClickDelete = PublishSubject.create<Void>()

    override fun onActivityCreated() {
        setupTopBar()
        setupPager()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (mAllowDelete) {
            menuInflater.inflate(R.menu.menu_activity_plan_details, menu)
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
        return R.layout.activity_plan_details
    }

    override fun getBindingView(): View? {
        binding = ActivityPlanDetailsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun onClickEdit(): Observable<Void> {
        return mOnClickEdit
    }

    override fun onClickDelete(): Observable<Void> {
        return mOnClickDelete
    }

    override fun onClickToggleStartPlan(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.toggleStartBtn))
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        toggleShimmerLayout(binding.root.findViewById(R.id.topShimmerContainer), show, false)
        toggleShimmerLayout(binding.root.findViewById(R.id.bottomShimmerContainer), show, false, R.color.background_toolbar)
    }

    override fun loadPlanDetails(plan: ELPlan, showStop: Boolean) {
        binding.root.findViewById<SubtitleCollapsingToolbarLayout>(R.id.toolbar_layout).subtitle = (String.format("%s  •  %s  •  %s", plan.name, resources.getQuantityString(R.plurals.weeks, plan.weeks.size, plan.weeks.size), resources.getQuantityString(R.plurals.days, plan.getDaysCount(), plan.getDaysCount())))
        binding.root.findViewById<ScrollingPagerIndicator>(R.id.indicator).visibility = if (plan.weeks.size > 1) View.VISIBLE else View.GONE
        binding.root.findViewById<Button>(R.id.toggleStartBtn).visibility = View.VISIBLE
        // Reattach our pager adapter if the plan changed
        mAdapter = WeekPagerAdapter(supportFragmentManager)
        binding.pager.adapter = mAdapter
        binding.pager.currentItem = 0
        binding.root.findViewById<ScrollingPagerIndicator>(R.id.indicator).reattach()
        mAdapter?.setPlan(plan)
        setPageTitle()
        // Photo
        if (plan.imageUrl != null) {
            ELGlideModule.loadImage(plan.imageUrl, binding.root.findViewById(R.id.coverImage))
        } else {
            binding.root.findViewById<ImageView>(R.id.coverImage).setImageDrawable(null)
        }
        mAllowDelete = plan.isEditable()
        invalidateOptionsMenu()
        // Start/Stop
        binding.root.findViewById<Button>(R.id.toggleStartBtn).setText(if (showStop) R.string.plan_details_stop else R.string.plan_details_start)
        binding.root.findViewById<Button>(R.id.toggleStartBtn).setBackgroundResource(if (showStop) R.drawable.rounded_corners_btn_three else R.drawable.rounded_corners_btn_one)
        binding.root.findViewById<Button>(R.id.toggleStartBtn).setTextColor(ContextCompat.getColor(this, if (showStop) R.color.main_accent else R.color.background_card))
    }

    override fun getItemToEditUuid(): String? {
        return intent.getStringExtra(EXTRA_PLAN_UUID)
    }

    private fun setPageTitle() {
        binding.root.findViewById<SubtitleCollapsingToolbarLayout>(R.id.toolbar_layout).title = getString(R.string.plan_details_week, (binding.pager.currentItem + 1).toString())
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterPlanDetails()
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_PLAN_DETAILS
    }

    private fun setupTopBar() {
        setSupportActionBar(binding.root.findViewById(R.id.toolbar))
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.root.findViewById<SubtitleCollapsingToolbarLayout>(R.id.toolbar_layout).title = ""
        binding.root.findViewById<SubtitleCollapsingToolbarLayout>(R.id.toolbar_layout).subtitle = ""
    }

    private fun setupPager() {
        mAdapter = WeekPagerAdapter(supportFragmentManager)
        binding.pager.adapter = mAdapter
        binding.pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
                // No-op
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
                // No-op
            }

            override fun onPageSelected(p0: Int) {
                setPageTitle()
            }

        })
        binding.root.findViewById<ScrollingPagerIndicator>(R.id.indicator).attachToPager(binding.pager)
    }

    private class WeekPagerAdapter(fragmentManager: FragmentManager) : ELFragmentPagerAdapter(fragmentManager) {

        private var mPlan: ELPlan? = null

        fun setPlan(plan: ELPlan) {
            mPlan = plan
            val frags = ArrayList<Fragment>()
            for (i in plan.weeks.indices) {
                val weekFragment = WeekDaysFragment()
                weekFragment.setData(mPlan, i)
                frags.add(weekFragment)
            }
            setItems(frags)
        }
    }
}