package com.everlog.ui.onboarding

import android.view.View
import com.everlog.R
import com.everlog.managers.OnboardManager
import com.everlog.ui.activities.home.workout.WorkoutActivity
import com.getkeepsafe.taptargetview.TapTargetView

class WorkoutOnboardingController(context: WorkoutActivity) : ExerciseGroupsOnboardingController<WorkoutActivity>(context) {

    override fun doCheckOnboarding() {
        if (!onboardingViewsVisible()) {
            return
        }
        val goalView = getMuscleGoalView()
        val timerView = getExerciseTimerView()
        if (!OnboardManager.manager.seenOnboard(OnboardManager.PreferenceKeys.SEEN_ONBOARD_WORKOUT_GOALS) && goalView != null) {
            val target = buildTapTarget(goalView,
                    "Muscle Training Goals",
                    "Set a training goal and take your workouts to the next level with our optimised weight suggestions",
                    1)
            if (showTarget(target, object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView?) {
                    super.onTargetClick(view)
                    checkOnboarding()
                }
            })) {
                OnboardManager.manager.setSeenOnboard(OnboardManager.PreferenceKeys.SEEN_ONBOARD_WORKOUT_GOALS, true)
            }
        } else if (!OnboardManager.manager.seenOnboard(OnboardManager.PreferenceKeys.SEEN_ONBOARD_WORKOUT_SET_TIMER) && timerView != null) {
            val target = buildTapTarget(timerView,
                    "Exercise Timer",
                    "Tap to start a timer for this exercise",
                    1)
            if (showTarget(target, object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView?) {
                    super.onTargetClick(view)
                    checkOnboarding()
                }
            })) {
                OnboardManager.manager.setSeenOnboard(OnboardManager.PreferenceKeys.SEEN_ONBOARD_WORKOUT_SET_TIMER, true)
            }
        } else {
            super.doCheckOnboarding()
        }
    }

    override fun onboardingViewsVisible(): Boolean {
        return super.onboardingViewsVisible() && getActivity().findViewById<View>(R.id.recyclerView)?.visibility == View.VISIBLE
    }

    private fun getExerciseTimerView(): View? {
        val holder = getExerciseViewHolder()
        val view = holder?.binding?.startTimerBtn
        val panel = holder?.binding?.timePanel
        return if (panel?.visibility == View.VISIBLE && view?.visibility == View.VISIBLE) view else null
    }

    private fun getMuscleGoalView(): View? {
        if (mAtTop) {
            // We could have added a super-set with many exercises causing the muscle goal to be out of the viewport
            return getActivity().findViewById(R.id.muscleGoalManageLbl)
        }
        return null
    }
}