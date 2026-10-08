package com.everlog.ui.activities.onboarding

import com.everlog.managers.analytics.AnalyticsManager

/**
 * The onboarding's analytics events, so the view-model can be tested without Firebase. A step is
 * the welcome, a question (by its id), building, the reveal or saving.
 */
interface OnboardingAnalytics {
    // The first time a step shows
    fun stepViewed(step: String)

    // Let's go, a question's Next (with its answer), building or saving done, or a reveal button
    fun stepCompleted(step: String, value: String? = null)

    // A question's Skip, or Skip after building or saving failed
    fun stepSkipped(step: String)

    fun stepFailed(step: String)

    fun stepRetried(step: String)

    // An answered question's summary row tapped to change it
    fun questionReopened(step: String)

    // Done on a reopened question, with the new answer
    fun questionEdited(step: String, value: String?)

    fun skipPromptShown(step: String)

    fun skipPromptCancelled(step: String)

    // A template's card on the reveal opened or closed
    fun templateToggled(open: Boolean)

    // How the onboarding ended, at which step, and how many templates it saved
    fun finished(outcome: String, step: String, routines: Int)
}

class RealOnboardingAnalytics : OnboardingAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun stepViewed(step: String) = analytics.onboardingStepViewed(step)

    override fun stepCompleted(step: String, value: String?) = analytics.onboardingStepCompleted(step, value)

    override fun stepSkipped(step: String) = analytics.onboardingStepSkipped(step)

    override fun stepFailed(step: String) = analytics.onboardingStepFailed(step)

    override fun stepRetried(step: String) = analytics.onboardingStepRetried(step)

    override fun questionReopened(step: String) = analytics.onboardingQuestionReopened(step)

    override fun questionEdited(step: String, value: String?) = analytics.onboardingQuestionEdited(step, value)

    override fun skipPromptShown(step: String) = analytics.onboardingSkipPromptShown(step)

    override fun skipPromptCancelled(step: String) = analytics.onboardingSkipPromptCancelled(step)

    override fun templateToggled(open: Boolean) = analytics.onboardingTemplateToggled(open)

    override fun finished(outcome: String, step: String, routines: Int) = analytics.onboardingFinished(outcome, step, routines)
}
