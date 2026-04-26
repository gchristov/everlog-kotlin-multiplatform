package com.everlog.ui.activities.home

import android.content.Intent
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
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
import com.jakewharton.rxbinding.view.RxView
import rx.Observable

class HomeActivity : BaseActivity(), MvpViewHome {

    private var mPresenter: PresenterHome? = null
    private lateinit var binding: ActivityHomeBinding

    private var mAdapter: ELFragmentPagerAdapter? = null
    private var mIndexMapTab = mapOf(Pair(0, 0), Pair(1, 1), Pair(2, 2), Pair(3, 2), Pair(4, 3))
    private var mIndexMapPager = mapOf(Pair(0, 0), Pair(1, 1), Pair(2, 3), Pair(3, 4))
    private var mIndexMapTabId = mapOf(Pair(0, R.id.action_week), Pair(1, R.id.action_workouts), Pair(2, R.id.action_activity), Pair(3, R.id.action_settings))
    private var mIndexMapTabIdReverse = mapOf(Pair(R.id.action_week, 0), Pair(R.id.action_workouts, 1), Pair(R.id.action_activity, 2), Pair(R.id.action_settings, 3))

    override fun onActivityCreated() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.tabBar.updatePadding(bottom = systemBars.bottom)
            binding.addBtn.updateLayoutParams<RelativeLayout.LayoutParams> {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.margin_18) + systemBars.bottom
            }
            insets
        }
        ViewCompat.setZ(binding.addBtn, binding.tabBar.z + 1)
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

    override fun onBackPressed() {
        if (binding.pager.currentItem != 0) {
            binding.pager.setCurrentItem(0, true)
        } else {
            super.onBackPressed()
        }
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

    override fun onClickAdd(): Observable<Void> {
        return RxView.clicks(binding.addBtn)
    }

    override fun showWeek() {
        binding.pager.setCurrentItem(0, true)
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
        binding.addBtn.performClick()
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
        binding.pager.setCurrentItem(mIndexMapTab[tabIndex]!!, false)
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
        binding.tabBar.setOnNavigationItemSelectedListener {
            val index = mIndexMapTabIdReverse[it.itemId]
            if (index == null) {
                binding.addBtn.performClick()
            } else if (binding.pager.currentItem != index) {
                binding.pager.setCurrentItem(index, true)
            }
            true
        }
    }
}