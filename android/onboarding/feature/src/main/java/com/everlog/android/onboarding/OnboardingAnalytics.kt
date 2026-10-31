package com.everlog.android.onboarding


/**
 * The onboarding's analytics events, so the view-model can be tested without Firebase. A step is
 * the welcome, a question (by its id), building, the reveal or saving.
 */
interface OnboardingAnalytics {
    // Each time a step's screen shows, unlike stepViewed
    fun screenViewed(step: OnboardingStep)

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

// The values the onboarding's analytics events carry
object OnboardingAnalyticsValues {
    // Onboarding steps, besides the questions, which are their question's id (e.g. units, days)
    const val STEP_WELCOME = "welcome"
    const val STEP_BUILDING = "building"
    const val STEP_REVEAL = "reveal"
    const val STEP_SAVING = "saving"

    // The reveal's buttons, as its step's completed value
    const val REVEAL_LOOKS_GOOD = "looks_good"
    const val REVEAL_BUILD_OWN = "build_own"

    // How the onboarding ended: the starter templates saved, the user's own template saved, or
    // skipped (Skip setup, or Skip after building or saving failed)
    const val OUTCOME_STARTER_TEMPLATES = "starter_templates"
    const val OUTCOME_OWN_TEMPLATE = "own_template"
    const val OUTCOME_SKIPPED = "skipped"
}

// The names analytics already has for the onboarding's screens
object OnboardingScreenNames {
    const val WELCOME = "screen_onboarding_welcome"
    const val QUESTIONS = "screen_onboarding_questions"
    const val BUILDING = "screen_onboarding_building"
    const val REVEAL = "screen_onboarding_reveal"
    const val SAVING = "screen_onboarding_saving"
}
