package com.everlog.managers.apprate

/**
 * Keeps the rating prompt's progress, shared by every [RatePromptTrigger]: qualifying actions since the
 * last prompt, how many prompts there have been, and when the last one was.
 */
class RatePromptManager(private val pacing: RatePromptPacing = RatePromptPacing()) {

    companion object {

        @JvmField
        val manager = RatePromptManager()
    }

    /**
     * Counts a qualifying action towards the next prompt, and returns whether the prompt is now due. An
     * action reported again with the same [actionId] (see [RatePromptTrigger.actionId]) isn't counted twice.
     */
    fun recordAction(actionId: String, now: Long): Boolean {
        if (actionId != AppLaunchState.state.ratePromptLastActionId()) {
            AppLaunchState.state.setRatePromptActionRecorded(AppLaunchState.state.ratePromptActionsSinceLast() + 1, actionId)
        }
        return isDue(now)
    }

    fun isDue(now: Long): Boolean {
        return pacing.isDue(
            actionsSinceLastPrompt = AppLaunchState.state.ratePromptActionsSinceLast(),
            promptsSoFar = AppLaunchState.state.ratePromptCount(),
            lastPromptDate = AppLaunchState.state.ratePromptLastDate(),
            now = now
        )
    }

    /**
     * Records that the prompt was launched, starting the count towards the next one. Returns which
     * prompt this was, starting at 1.
     */
    fun promptLaunched(now: Long): Int {
        val promptNumber = AppLaunchState.state.ratePromptCount() + 1
        AppLaunchState.state.setRatePromptLaunched(promptNumber, now)
        return promptNumber
    }
}
