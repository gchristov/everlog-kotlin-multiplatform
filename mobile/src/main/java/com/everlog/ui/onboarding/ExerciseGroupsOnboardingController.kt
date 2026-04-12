package com.everlog.ui.onboarding

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.everlog.R
import com.everlog.managers.OnboardManager
import com.everlog.ui.activities.home.exercisegroup.CreateExerciseGroupsActivity
import com.everlog.ui.adapters.exercise.group.ExerciseGroupCreateAdapter
import com.getkeepsafe.taptargetview.TapTargetView
import kotlin.math.max

open class ExerciseGroupsOnboardingController<T : CreateExerciseGroupsActivity> constructor(context: T): BaseOnboardingController<T>(context) {

    protected var mAtTop = true

    fun setAtTop(atTop: Boolean) {
        mAtTop = atTop
    }

    override fun doCheckOnboarding() {
        if (!onboardingViewsVisible()) {
            return
        }
        val targetOptionsExercise = getExerciseOptionsView()
        val targetOptionsSet = getSetOptionsView()
        if (!OnboardManager.manager.seenOnboard(OnboardManager.PreferenceKeys.SEEN_ONBOARD_GROUPS_OPTIONS_EXERCISE) && targetOptionsExercise != null) {
            val target = buildTapTarget(targetOptionsExercise,
                    "Combine Exercises",
                    "Tap to combine two or more exercises into super-sets or to reorder",
                    1)
            if (showTarget(target, object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView?) {
                    super.onTargetClick(view)
                    targetOptionsExercise.performClick()
                }
            })) {
                OnboardManager.manager.setSeenOnboard(OnboardManager.PreferenceKeys.SEEN_ONBOARD_GROUPS_OPTIONS_EXERCISE, true)
            }
        } else if (!OnboardManager.manager.seenOnboard(OnboardManager.PreferenceKeys.SEEN_ONBOARD_GROUPS_OPTIONS_SET) && targetOptionsSet != null) {
            val target = buildTapTarget(targetOptionsSet,
                    "Manage Sets",
                    "Tap to duplicate or delete sets",
                    1)
            if (showTarget(target, object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView?) {
                    super.onTargetClick(view)
                    targetOptionsSet.performClick()
                }
            })) {
                OnboardManager.manager.setSeenOnboard(OnboardManager.PreferenceKeys.SEEN_ONBOARD_GROUPS_OPTIONS_SET, true)
            }
        }
    }

    internal open fun onboardingViewsVisible(): Boolean {
        val list = getActivity().getListView()
        return !getActivity().isInSelectionMode()
                && list?.adapter?.itemCount ?: 0 > 0
    }

    private fun getSetOptionsView(): View? {
        val holder = getSetViewHolder()
        return holder?.findViewById(R.id.menuBtn)
    }

    private fun getExerciseOptionsView(): View? {
        val holder = getExerciseViewHolder()
        return holder?.binding?.exerciseImg
    }

    private fun getSelectExerciseView(): View? {
        val holder = getExerciseViewHolder()
        return holder?.binding?.exerciseContainer
    }

    private fun getExerciseGroupViewHolder(): ExerciseGroupCreateAdapter.GroupViewHolder? {
        val list = getActivity().getListView()
        var position = if (mAtTop) 0 else (list?.adapter?.itemCount ?: 0) - 1
        position = max(position, 0)
        list?.getChildAt(position)?.let {
            return list.getChildViewHolder(it) as? ExerciseGroupCreateAdapter.GroupViewHolder
        } ?: run {
            return null
        }
    }

    internal fun getSetViewHolder(): View? {
        val pager = getExerciseGroupViewHolder()?.getPager()
        var position = if (mAtTop) 0 else (pager?.adapter?.count ?: 0) - 1
        position = max(position, 0)
        return pager?.getChildAt(position)
    }

    internal fun getExerciseViewHolder(): ExerciseGroupCreateAdapter.ExerciseViewHolder? {
        val list = getSetViewHolder()?.findViewById<RecyclerView>(R.id.recyclerView)
        var position = if (mAtTop) 0 else (list?.adapter?.itemCount ?: 0) - 1
        position = max(position, 0)
        list?.getChildAt(position)?.let {
            return list.getChildViewHolder(it) as? ExerciseGroupCreateAdapter.ExerciseViewHolder
        } ?: run {
            return null
        }
    }
}
