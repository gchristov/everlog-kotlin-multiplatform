package com.everlog.ui.activities.onboarding

import com.everlog.android.common.kotlin.WeightUnit

internal class FakeOnboardingAnalytics : OnboardingAnalytics {
    val events = mutableListOf<String>()
    val screens = mutableListOf<OnboardingStep>()

    override fun screenViewed(step: OnboardingStep) { screens += step }

    override fun stepViewed(step: String) { events += "viewed $step" }
    override fun stepCompleted(step: String, value: String?) { events += "completed $step $value" }
    override fun stepSkipped(step: String) { events += "skipped $step" }
    override fun stepFailed(step: String) { events += "failed $step" }
    override fun stepRetried(step: String) { events += "retried $step" }
    override fun questionReopened(step: String) { events += "reopened $step" }
    override fun questionEdited(step: String, value: String?) { events += "edited $step $value" }
    override fun skipPromptShown(step: String) { events += "prompt shown $step" }
    override fun skipPromptCancelled(step: String) { events += "prompt cancelled $step" }
    override fun templateToggled(open: Boolean) { events += "toggled $open" }
    override fun finished(outcome: String, step: String, routines: Int) { events += "finished $outcome $step $routines" }
}

internal class FakeOnboardingSettings : OnboardingSettings {
    var savedUnit: WeightUnit? = null
    var savedWeeklyGoal: Int? = null

    override fun setWeightUnit(unit: WeightUnit) {
        savedUnit = unit
    }

    override fun setWeeklyWorkoutsGoal(count: Int) {
        savedWeeklyGoal = count
    }
}
