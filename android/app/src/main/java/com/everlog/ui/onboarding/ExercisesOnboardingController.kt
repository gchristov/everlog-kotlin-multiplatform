package com.everlog.ui.onboarding

import android.view.View
import com.everlog.android.common.mvp.onboarding.BaseOnboardingController
import com.everlog.android.common.mvp.onboarding.OnboardManager
import com.everlog.ui.activities.home.exercise.ExercisesActivity
import com.getkeepsafe.taptargetview.TapTargetView

private const val SEEN_ONBOARD_EXERCISES_FILTER = "SEEN_ONBOARD_EXERCISES_FILTER"

class ExercisesOnboardingController constructor(context: ExercisesActivity): BaseOnboardingController<ExercisesActivity>(context) {

    override fun doCheckOnboarding() {
        if (!onboardingViewsVisible()) {
            return
        }
        if (!OnboardManager.manager.seenOnboard(SEEN_ONBOARD_EXERCISES_FILTER)) {
            val target = buildTapTarget(getFiltersView()!!,
                    "Filter Exercises",
                    "Tap to filter exercises based on muscle group",
                    1)
            if (showTarget(target, object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView?) {
                    super.onTargetClick(view)
                    getFiltersView()?.performClick()
                }
            })) {
                OnboardManager.manager.setSeenOnboard(SEEN_ONBOARD_EXERCISES_FILTER, true)
            }
        }
    }

    private fun onboardingViewsVisible(): Boolean {
        return getFiltersView() != null
    }

    private fun getFiltersView(): View? {
        return getActivity().getFiltersMenu()
    }
}