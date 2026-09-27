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
     * Counts a qualifying action towards the next prompt, and returns whether the prompt is now due.
     */
    fun recordAction(now: Long): Boolean {
        AppLaunchState.state.setRatePromptActionsSinceLast(AppLaunchState.state.ratePromptActionsSinceLast() + 1)
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
