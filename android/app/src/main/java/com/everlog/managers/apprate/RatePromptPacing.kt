package com.everlog.managers.apprate

import com.everlog.config.AppConfig
import java.util.concurrent.TimeUnit

/**
 * Decides whether the user is due a rating prompt, whichever trigger asked.
 *
 * Each prompt needs the next goal in [actionGoals] of qualifying actions since the previous one, and
 * at least [cooldownMillis] since it. Once every goal has been used, the user is never asked again.
 */
class RatePromptPacing(
    private val actionGoals: List<Int> = AppConfig.configuration.ratePromptActionGoals,
    private val cooldownMillis: Long = TimeUnit.DAYS.toMillis(AppConfig.configuration.ratePromptCooldownDays.toLong())
) {

    fun isDue(actionsSinceLastPrompt: Int, promptsSoFar: Int, lastPromptDate: Long, now: Long): Boolean {
        val goal = actionGoals.getOrNull(promptsSoFar) ?: return false
        if (actionsSinceLastPrompt < goal) {
            return false
        }
        return lastPromptDate <= 0 || now - lastPromptDate >= cooldownMillis
    }
}
