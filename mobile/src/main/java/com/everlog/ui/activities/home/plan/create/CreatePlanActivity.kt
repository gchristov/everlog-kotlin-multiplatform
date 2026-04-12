package com.everlog.ui.activities.home.plan.create

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.everlog.R
import com.everlog.constants.ELConstants.EXTRA_PLAN_UUID
import com.everlog.data.model.plan.ELPlan
import com.everlog.databinding.ActivityPlanCreateBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.auth.LocalUserManager
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.views.notification.warning.WarningNotificationView
import com.everlog.utils.glide.ELGlideModule
import com.facebook.shimmer.ShimmerFrameLayout
import com.jakewharton.rxbinding.view.RxView
import com.jakewharton.rxbinding.widget.RxTextView
import rx.Observable

class CreatePlanActivity : BaseActivity(), MvpViewCreatePlan {

    enum class Flow {
        INFO, WEEKS, DAYS
    }

    private var mPresenter: PresenterCreatePlan? = null
    private lateinit var binding: ActivityPlanCreateBinding

    override fun onActivityCreated() {
        setupTopBar()
        setupListView()
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_plan_create
    }

    override fun getBindingView(): View? {
        binding = ActivityPlanCreateBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_PLAN_CREATE
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun loadPlanDetails(plan: ELPlan) {
        // Name
        binding.root.findViewById<EditText>(R.id.nameField).setText(plan.name)
        // Photo
        binding.root.findViewById<ImageView>(R.id.photoPrompt).visibility = if (plan.imageUrl != null) View.GONE else View.VISIBLE
        if (plan.imageUrl != null) {
            ELGlideModule.loadImage(plan.imageUrl, binding.root.findViewById(R.id.imageView))
        } else {
            binding.root.findViewById<ImageView>(R.id.imageView).setImageDrawable(null)
        }
        // Weeks
        binding.root.findViewById<TextView>(R.id.weeksField).text = if (plan.hasWeeksWithWorkouts()) resources.getQuantityString(R.plurals.weeks, plan.weeks.size, plan.weeks.size) else getString(R.string.tap_to_setup)
        // Pro upgrade
        binding.daysContainer.proUpgradePrompt.setType(WarningNotificationView.WarningType.PRO_PLAN_DAYS)
        binding.daysContainer.proUpgradePrompt.visibility = if (LocalUserManager.getUser()?.isPro() == true) View.GONE else View.VISIBLE
    }

    override fun getItemToEditUuid(): String? {
        return intent.getStringExtra(EXTRA_PLAN_UUID)
    }

    override fun onClickSave(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.saveBtn))
    }

    override fun onClickCover(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.coverImage))
    }

    override fun onClickWeeks(): Observable<Void> {
        return RxView.clicks(binding.root.findViewById(R.id.weeksBtn))
    }

    override fun onClickAddWeek(): Observable<Void> {
        return RxView.clicks(binding.weeksContainer.addBtn)
    }

    override fun getPlanName(): String {
        return binding.root.findViewById<EditText>(R.id.nameField).text.toString()
    }

    override fun onNameChanged(): Observable<CharSequence> {
        return RxTextView.textChanges(binding.root.findViewById(R.id.nameField))
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        toggleShimmerLayout(binding.root.findViewById(R.id.infoContainer), show, false)
    }

    override fun togglePanel(flow: Flow, weekNumber: Int) {
        when (flow) {
            Flow.INFO -> {
                supportActionBar?.title = getString(if (isEditMode()) R.string.create_plan_title_edit else R.string.create_plan_title)
                binding.root.findViewById<ShimmerFrameLayout>(R.id.infoContainer).visibility = View.VISIBLE
                binding.weeksContainer.root.visibility = View.GONE
                binding.daysContainer.root.visibility = View.GONE
                binding.weeksContainer.weeksList.layoutManager?.scrollToPosition(0)
                binding.root.findViewById<Button>(R.id.saveBtn).setText(R.string.save)
                binding.root.findViewById<Button>(R.id.saveBtn).setBackgroundResource(R.drawable.rounded_corners_btn_one)
                binding.root.findViewById<Button>(R.id.saveBtn).setTextColor(ContextCompat.getColor(this, R.color.background_card))
            }
            Flow.WEEKS -> {
                supportActionBar?.title = getString(R.string.create_plan_weeks)
                binding.root.findViewById<ShimmerFrameLayout>(R.id.infoContainer).visibility = View.GONE
                binding.weeksContainer.root.visibility = View.VISIBLE
                binding.daysContainer.root.visibility = View.GONE
                binding.weeksContainer.weeksList.layoutManager?.scrollToPosition(0)
                binding.root.findViewById<Button>(R.id.saveBtn).setText(R.string.done)
                binding.root.findViewById<Button>(R.id.saveBtn).setBackgroundResource(R.drawable.rounded_corners_btn_three)
                binding.root.findViewById<Button>(R.id.saveBtn).setTextColor(ContextCompat.getColor(this, R.color.main_accent))
            }
            Flow.DAYS -> {
                supportActionBar?.title = String.format("Week %d", weekNumber + 1)
                binding.root.findViewById<ShimmerFrameLayout>(R.id.infoContainer).visibility = View.GONE
                binding.weeksContainer.root.visibility = View.GONE
                binding.daysContainer.root.visibility = View.VISIBLE
                binding.daysContainer.daysList.layoutManager?.scrollToPosition(0)
                binding.root.findViewById<Button>(R.id.saveBtn).setText(R.string.done)
                binding.root.findViewById<Button>(R.id.saveBtn).setBackgroundResource(R.drawable.rounded_corners_btn_three)
                binding.root.findViewById<Button>(R.id.saveBtn).setTextColor(ContextCompat.getColor(this, R.color.main_accent))
            }
        }
    }

    private fun isEditMode(): Boolean {
        return getItemToEditUuid() != null
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterCreatePlan()
    }

    private fun setupTopBar() {
        binding.root.findViewById<Toolbar>(R.id.toolbar).setNavigationIcon(R.drawable.ic_clear_white)
        setSupportActionBar(binding.root.findViewById(R.id.toolbar))
        supportActionBar?.title = getString(if (isEditMode()) R.string.create_plan_title_edit else R.string.create_plan_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupListView() {
        // Weeks
        val mWeeksLayoutManager = LinearLayoutManager(this)
        binding.weeksContainer.weeksList.layoutManager = mWeeksLayoutManager
        binding.weeksContainer.weeksList.adapter = mPresenter?.getWeeksListAdapter()
        // Days
        val mDaysLayoutManager = LinearLayoutManager(this)
        binding.daysContainer.daysList.layoutManager = mDaysLayoutManager
        binding.daysContainer.daysList.adapter = mPresenter?.getDaysListAdapter()
    }
}