package com.everlog.ui.activities.home.exercisegroup

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.dialog.DialogBuilder
import com.everlog.ui.onboarding.ExerciseGroupsOnboardingController
import com.everlog.utils.Utils
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import rx.subjects.PublishSubject
import java.io.Serializable

abstract class CreateExerciseGroupsActivity : BaseActivity(), MvpViewCreateExerciseGroups {

    internal var mOnboardingController: ExerciseGroupsOnboardingController<*>? = null

    private val mOnCancelSelectionClick = PublishSubject.create<Void>()
    private val mOnSelectionInfoClick = PublishSubject.create<Void>()
    private val mOnSelectionLinkClick = PublishSubject.create<Void>()
    private val mOnSelectionDeleteClick = PublishSubject.create<Void>()

    abstract fun getExerciseGroupsPresenter(): PresenterCreateExerciseGroups<*>?

    override fun onActivityCreated() {
        setupTopBar()
        setupListView()
        setupOnboarding()
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return getExerciseGroupsPresenter() as? BaseActivityPresenter<T>
    }

    override fun onClickSave(): Observable<Void> {
        return RxView.clicks(findViewById(R.id.saveBtn))
    }

    override fun onClickAdd(): Observable<Void> {
        return RxView.clicks(findViewById(R.id.addBtn))
    }

    override fun onClickSelectionCancel(): Observable<Void> {
        return mOnCancelSelectionClick
    }

    override fun onClickSelectionLink(): Observable<Void> {
        return mOnSelectionLinkClick
    }

    override fun onClickSelectionInfo(): Observable<Void> {
        return mOnSelectionInfoClick
    }

    override fun onClickSelectionDelete(): Observable<Void> {
        return mOnSelectionDeleteClick
    }

    override fun getOnboardingController(): ExerciseGroupsOnboardingController<*>? {
        return mOnboardingController
    }

    override fun getItemsToEdit(): List<ELExerciseGroup>? {
        return intent?.extras?.getSerializable(ELConstants.EXTRA_EXERCISE_GROUPS) as? List<ELExerciseGroup>
    }

    override fun showPickerDuration(value: Int, type: DialogBuilder.DurationPickerDialogType): Observable<Int> {
        return DialogBuilder.showPickerDurationDialog(this, value, type)
    }

    override fun toggleContextToolbar(visible: Boolean, selectedCount: Int) {
        val targetVisibility = if (visible) View.VISIBLE else View.GONE
        if (findViewById<View>(R.id.optionsToolbarContainer).visibility != targetVisibility) {
            findViewById<View>(R.id.optionsToolbarContainer).visibility = targetVisibility
        }
        if (selectedCount > 0) {
            findViewById<Toolbar>(R.id.optionsToolbar).title = "$selectedCount"
            findViewById<Toolbar>(R.id.optionsToolbar).menu.findItem(R.id.action_info)?.isVisible = selectedCount == 1
        }
    }

    override fun scrollToBottom() {
        Utils.runWithDelay({
            findViewById<RecyclerView>(R.id.recyclerView).scrollToPosition((findViewById<RecyclerView>(R.id.recyclerView).adapter?.itemCount ?: 1) - 1)
        }, 300)
    }

    fun getListView(): RecyclerView? {
        return findViewById(R.id.recyclerView)
    }

    fun isInSelectionMode(): Boolean {
        return getExerciseGroupsPresenter()?.isInSelectionMode() ?: false
    }

    // Setup

    protected open fun setupTopBar() {
        findViewById<Toolbar>(R.id.toolbar).setNavigationIcon(R.drawable.ic_clear_white)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "[YOUR TITLE]"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Selection options
        findViewById<Toolbar>(R.id.optionsToolbar).setNavigationIcon(R.drawable.ic_clear_white)
        findViewById<Toolbar>(R.id.optionsToolbar).inflateMenu(R.menu.menu_activity_exercise_group_create)
        findViewById<Toolbar>(R.id.optionsToolbar).setNavigationOnClickListener {
            mOnCancelSelectionClick.onNext(null)
        }
        findViewById<Toolbar>(R.id.optionsToolbar).setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_info -> mOnSelectionInfoClick.onNext(null)
                R.id.action_link -> mOnSelectionLinkClick.onNext(null)
                R.id.action_delete -> mOnSelectionDeleteClick.onNext(null)
            }
            true
        }
    }

    internal open fun setupListView() {
        val layoutManager = LinearLayoutManager(this)
        findViewById<RecyclerView>(R.id.recyclerView).layoutManager = layoutManager
        // Setup Drag-and-Drop
        val adapter = getExerciseGroupsPresenter()?.getListAdapter()
        adapter?.itemTouchHelper?.attachToRecyclerView(findViewById(R.id.recyclerView))
        findViewById<RecyclerView>(R.id.recyclerView).adapter = adapter
    }

    internal open fun setupOnboarding() {
        mOnboardingController = ExerciseGroupsOnboardingController(this)
    }
}

class DefaultCreateExerciseGroupsActivity : CreateExerciseGroupsActivity() {

    private var mPresenter: DefaultPresenterCreateExerciseGroups? = null

    companion object {

        class Properties {
            var groups: List<ELExerciseGroup>? = null
                private set
            fun groups(groups: List<ELExerciseGroup>?) = apply { this.groups = groups }
        }

        @JvmStatic
        fun launchIntent(context: Context, properties: Properties): Intent {
            val intent = Intent(context, DefaultCreateExerciseGroupsActivity::class.java)
            intent.putExtra(ELConstants.EXTRA_EXERCISE_GROUPS, properties.groups as Serializable)
            return intent
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_exercise_groups_create
    }
    
    override fun getExerciseGroupsPresenter(): PresenterCreateExerciseGroups<*>? {
        return mPresenter
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_EXERCISE_GROUPS_CREATE
    }

    // Setup
    
    override fun setupPresenter() {
        mPresenter = DefaultPresenterCreateExerciseGroups()
    }

    override fun setupTopBar() {
        super.setupTopBar()
        supportActionBar?.title = getString(R.string.exercises_title_edit)
    }
}