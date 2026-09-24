package com.everlog.ui.activities.home

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.databinding.ActivityHomeBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.managers.firebase.FirebaseStorageManager
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.fragments.home.activity.ActivityHomeFragment
import com.everlog.ui.fragments.home.settings.SettingsHomeFragment
import com.everlog.ui.fragments.home.week.WeekHomeFragment
import com.everlog.ui.fragments.home.workouts.WorkoutsHomeFragment
import com.everlog.ui.views.viewpager.ELFragmentPagerAdapter
import com.everlog.utils.Utils
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import rx.subjects.PublishSubject

class HomeActivity : BaseActivity(), MvpViewHome {

    private var mPresenter: PresenterHome? = null
    private lateinit var binding: ActivityHomeBinding

    private var mAdapter: ELFragmentPagerAdapter? = null
    private var mIndexMapTabId = mapOf(Pair(0, R.id.action_week), Pair(1, R.id.action_workouts), Pair(2, R.id.action_activity), Pair(3, R.id.action_settings))
    private var mIndexMapTabIdReverse = mapOf(Pair(R.id.action_week, 0), Pair(R.id.action_workouts, 1), Pair(R.id.action_activity, 2), Pair(R.id.action_settings, 3))

    private val mOnClickAdd = PublishSubject.create<Void>()

    // App update

    private val mOnAppUpdateFlowResult = PublishSubject.create<Int>()
    private val mOnClickAppUpdateRestart = PublishSubject.create<Void>()
    private var mAppUpdateSnackbar: Snackbar? = null

    private val mAppUpdateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        mOnAppUpdateFlowResult.onNext(result.resultCode)
    }

    override fun onActivityCreated() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.tabBar.updatePadding(bottom = systemBars.bottom)
            insets
        }
        // APP STARTUP: Delay to not block
        Utils.runWithDelay({
            setupNavigation()
            animateAppearance()
        }, 10)
        Utils.runInBackground {
            // Resume any pending image uploads
            FirebaseStorageManager.resumePendingUploads()
            // Do app rate checks here because we might have to show it due to app launches trigger reached
            AppLaunchManager.manager.launchApp()
        }
    }

    override fun handleBackPressed(): Boolean {
        if (binding.pager.currentItem != 0) {
            binding.pager.setCurrentItem(0, true)
            return true
        }
        return super.handleBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Activity could have been killed and restarted
        if (mAdapter == null) {
            Utils.runWithDelay({
                activityResult(requestCode, resultCode, data)
            }, ELConstants.ACTIVITY_RESULT_DELAY)
        } else {
            activityResult(requestCode, resultCode, data)
        }
    }

    private fun activityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Notify fragments of result
        val count = mAdapter?.count ?: 0
        for (i in 0 until count) {
            mAdapter?.getItem(i)?.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_HOME
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_home
    }

    override fun getBindingView(): View? {
        binding = ActivityHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun onClickAddFab(): Observable<Void> {
        return RxView.clicks(binding.newWorkoutBtn)
    }

    override fun onClickAddWeekEmptyState(): Observable<Void> {
        return mOnClickAdd
    }

    override fun showWeek() {
        binding.pager.setCurrentItem(0, true)
    }

    override fun toggleStartWorkoutButton(visible: Boolean) {
        val btn = binding.newWorkoutBtn
        val isCurrentlyVisible = btn.visibility == View.VISIBLE
        if (visible && !isCurrentlyVisible) {
            btn.scaleX = 0f
            btn.scaleY = 0f
            btn.alpha = 0f
            btn.visibility = View.VISIBLE
            btn.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(OvershootInterpolator())
                    .start()
        } else if (!visible) {
            btn.animate().cancel()
            btn.visibility = View.GONE
        }
        // Hiding cancels any running offset animation, so reapply it without animating
        offsetStartWorkoutButtonForSnackbar(animate = false)
    }

    override fun appUpdateLauncher(): ActivityResultLauncher<IntentSenderRequest> {
        return mAppUpdateLauncher
    }

    override fun onAppUpdateFlowResult(): Observable<Int> {
        return mOnAppUpdateFlowResult
    }

    override fun onClickAppUpdateRestart(): Observable<Void> {
        return mOnClickAppUpdateRestart
    }

    override fun showAppUpdateReady() {
        if (mAppUpdateSnackbar?.isShownOrQueued == true) {
            return
        }
        mAppUpdateSnackbar = Snackbar.make(binding.root, R.string.app_update_ready, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.app_update_restart) { mOnClickAppUpdateRestart.onNext(null) }
                .setActionTextColor(ContextCompat.getColor(this, R.color.main_accent))
                .setAnchorView(binding.tabBar)
                .addCallback(object : Snackbar.Callback() {

                    override fun onShown(sb: Snackbar?) {
                        offsetStartWorkoutButtonForSnackbar(animate = true)
                    }

                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        mAppUpdateSnackbar = null
                        offsetStartWorkoutButtonForSnackbar(animate = true)
                    }
                })
        mAppUpdateSnackbar?.view?.let { styleAsCard(it) }
        mAppUpdateSnackbar?.show()
    }

    /**
     * The AppCompat theme gives snackbars the legacy full-width style, so make it a floating card.
     */
    private fun styleAsCard(snackbarView: View) {
        val margin = resources.getDimensionPixelSize(R.dimen.activity_margin)
        snackbarView.background = ContextCompat.getDrawable(this, R.drawable.rounded_corners_snackbar)
        snackbarView.elevation = resources.getDimension(R.dimen.snackbar_elevation)
        snackbarView.layoutParams = (snackbarView.layoutParams as ViewGroup.MarginLayoutParams).apply {
            setMargins(margin, margin, margin, margin)
        }
    }

    /**
     * The update snackbar sits right above the tab bar, so move the start workout button up to
     * sit above the snackbar while it's shown, and back down once it's gone.
     */
    private fun offsetStartWorkoutButtonForSnackbar(animate: Boolean) {
        val btn = binding.newWorkoutBtn
        val snackbarView = mAppUpdateSnackbar?.takeIf { it.isShown }?.view
        var offset = 0f
        if (snackbarView != null) {
            // Work out the button's resting position from the tab bar, as it may be hidden and not laid out
            val snackbarLocation = IntArray(2)
            val tabBarLocation = IntArray(2)
            snackbarView.getLocationInWindow(snackbarLocation)
            binding.tabBar.getLocationInWindow(tabBarLocation)
            val btnRestingBottom = tabBarLocation[1] - (btn.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
            val spacing = resources.getDimensionPixelSize(R.dimen.activity_margin)
            offset = minOf(0, snackbarLocation[1] - spacing - btnRestingBottom).toFloat()
        }
        if (animate) {
            // The interpolator is shared with the show animation's overshoot, so reset it
            btn.animate().translationY(offset).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        } else {
            btn.translationY = offset
        }
    }

    fun showPlans() {
        binding.pager.setCurrentItem(1, true)
    }

    fun showStatistics() {
        val pos = 2
        binding.pager.setCurrentItem(pos, true)
        val statsFragment = mAdapter?.getItem(pos) as? ActivityHomeFragment
        statsFragment?.showStatistics()
    }

    fun showSettings() {
        binding.pager.setCurrentItem(3, true)
    }

    fun showCreateActivity() {
        mOnClickAdd.onNext(null)
    }

    fun setWeekEmptyState(isEmpty: Boolean) {
        mPresenter?.setWeekIsEmpty(isEmpty)
    }

    private fun animateAppearance() {
        binding.pager.animate().setDuration(100).alpha(1f)
        binding.pager.animate().setDuration(200).translationY(0f)
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterHome()
    }

    private fun setupNavigation() {
        val fragments = ArrayList<Fragment>()
        fragments.add(WeekHomeFragment())
        fragments.add(WorkoutsHomeFragment())
        fragments.add(ActivityHomeFragment())
        fragments.add(SettingsHomeFragment())
        // Link pager
        mAdapter = ELFragmentPagerAdapter(supportFragmentManager, fragments)
        binding.pager.offscreenPageLimit = fragments.size
        binding.pager.adapter = mAdapter
        binding.pager.clearOnPageChangeListeners()
        // To restore any previous state in case the activity was killed
        val tabIndex = mPresenter?.getSelectedTab() ?: 0
        binding.tabBar.selectedItemId = mIndexMapTabId[tabIndex]!!
        binding.pager.setCurrentItem(tabIndex, false)
        binding.pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(p0: Int) {
                // No-op
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
                // No-op
            }

            override fun onPageSelected(p0: Int) {
                mPresenter?.setSelectedTab(p0)
                val pendingTabId = mIndexMapTabId[p0]
                val activeTabId = binding.tabBar.selectedItemId
                if (activeTabId != pendingTabId) {
                    binding.tabBar.selectedItemId = pendingTabId!!
                }
            }
        })
        // Link tab bar
        binding.tabBar.setOnItemSelectedListener {
            val index = mIndexMapTabIdReverse[it.itemId]
            if (index != null && binding.pager.currentItem != index) {
                binding.pager.setCurrentItem(index, true)
            }
            true
        }
    }
}