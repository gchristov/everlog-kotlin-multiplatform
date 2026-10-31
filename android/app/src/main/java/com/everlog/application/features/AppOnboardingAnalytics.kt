package com.everlog.application.features

import com.everlog.android.onboarding.OnboardingAnalytics
import com.everlog.android.onboarding.OnboardingScreenNames
import com.everlog.android.onboarding.OnboardingStep
import com.everlog.managers.analytics.AnalyticsManager

class AppOnboardingAnalytics : OnboardingAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun screenViewed(step: OnboardingStep) = analytics.screenName(
        when (step) {
            OnboardingStep.Welcome -> OnboardingScreenNames.WELCOME
            OnboardingStep.Questions -> OnboardingScreenNames.QUESTIONS
            OnboardingStep.Building -> OnboardingScreenNames.BUILDING
            OnboardingStep.Reveal -> OnboardingScreenNames.REVEAL
            OnboardingStep.Saving -> OnboardingScreenNames.SAVING
        },
    )

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
