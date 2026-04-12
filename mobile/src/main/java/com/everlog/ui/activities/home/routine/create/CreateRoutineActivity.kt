package com.everlog.ui.activities.home.routine.create

import android.content.Context
import android.content.Intent
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.data.model.ELRoutine
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.home.exercisegroup.CreateExerciseGroupsActivity
import com.everlog.ui.activities.home.exercisegroup.PresenterCreateExerciseGroups
import com.everlog.ui.dialog.DialogBuilder
import rx.Observable

class CreateRoutineActivity : CreateExerciseGroupsActivity(), MvpViewCreateRoutine {

    private var mPresenter: PresenterCreateRoutine? = null

    companion object {

        class Properties {
            var routine: ELRoutine? = null
                private set
            var showDetailsOnSuccess = false
                private set
            fun routine(routine: ELRoutine?) = apply { this.routine = routine }
            fun showDetailsOnSuccess(showDetailsOnSuccess: Boolean) = apply { this.showDetailsOnSuccess = showDetailsOnSuccess }
        }

        @JvmStatic
        fun launchIntent(context: Context, properties: Properties): Intent {
            val intent = Intent(context, CreateRoutineActivity::class.java)
            intent.putExtra(ELConstants.EXTRA_ROUTINE, properties.routine)
            intent.putExtra(ELConstants.EXTRA_SHOW_DETAILS_ON_SUCCESS, properties.showDetailsOnSuccess)
            return intent
        }
    }

    public override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_ROUTINE_CREATE
    }

    public override fun getLayoutResId(): Int {
        return R.layout.activity_routine_create
    }

    override fun getExerciseGroupsPresenter(): PresenterCreateExerciseGroups<*>? {
        return mPresenter
    }

    override fun getItemToEdit(): ELRoutine? {
        return intent.getSerializableExtra(ELConstants.EXTRA_ROUTINE) as? ELRoutine
    }

    override fun shouldOpenDetailsOnSuccess(): Boolean {
        return intent.getBooleanExtra(ELConstants.EXTRA_SHOW_DETAILS_ON_SUCCESS, true)
    }

    override fun showNamePrompt(name: String?): Observable<String> {
        return DialogBuilder.showInputStringDialog(this, name, DialogBuilder.StringDialogType.ROUTINE_NAME)
    }

    private fun isEditMode(): Boolean {
        return getItemToEdit() != null
    }

    // Setup

    public override fun setupPresenter() {
        mPresenter = PresenterCreateRoutine()
    }

    override fun setupTopBar() {
        super.setupTopBar()
        supportActionBar?.title = getString(if (isEditMode()) R.string.create_routine_title_edit else R.string.create_routine_title)
    }
}