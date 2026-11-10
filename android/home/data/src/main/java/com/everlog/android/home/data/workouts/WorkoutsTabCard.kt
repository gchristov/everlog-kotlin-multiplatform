package com.everlog.android.home.data.workouts

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.plan.data.PlanNextStep
import com.everlog.android.plan.data.currentWeek
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.nextStep

// The card on top of the Workouts tab
sealed interface WorkoutsTabCard {
    data class PlanNext(val plan: ELPlan, val routine: ELRoutine, val weekNumber: Int, val dayNumber: Int) : WorkoutsTabCard

    data class Suggested(val template: ELRoutine, val afterName: String, val times: Int, val of: Int) : WorkoutsTabCard

    data class StartHere(val template: ELRoutine) : WorkoutsTabCard

    data object GetStarted : WorkoutsTabCard
}

/**
 * The first card that applies: the ongoing plan's next day when it's a workout, a suggestion from [history], the
 * template saved first when none was done lately, or getting started without templates. Null for no card.
 * A plan whose next day is a rest day, or which is done, has no card here: starting a workout from the plan
 * completes its next day, whatever that day is, so the Week tab handles those.
 *
 * @param history Null while it's loading or couldn't load, which only leaves the plan and getting started.
 */
fun workoutsTabCard(templates: List<ELRoutine>, history: TemplateHistory?, ongoingPlan: ELPlan?): WorkoutsTabCard? {
    ongoingPlan?.planNext()?.let { return it }
    if (templates.isEmpty()) return WorkoutsTabCard.GetStarted
    history ?: return null
    val byUuid = templates.filter { it.uuid != null }.associateBy { it.uuid!! }
    suggestNextTemplate(history, byUuid.keys)?.let { suggestion ->
        val afterName = byUuid[suggestion.afterUuid]?.name ?: history.names[suggestion.afterUuid]
        if (afterName != null) {
            return WorkoutsTabCard.Suggested(byUuid.getValue(suggestion.uuid), afterName, suggestion.times, suggestion.of)
        }
    }
    if (history.order.isEmpty()) return WorkoutsTabCard.StartHere(templates.minBy { it.createdDate })
    return null
}

private fun ELPlan.planNext(): WorkoutsTabCard.PlanNext? {
    val step = nextStep() as? PlanNextStep.Workout ?: return null
    val week = currentWeek() ?: return null
    val day = week.nextDay ?: return null
    return WorkoutsTabCard.PlanNext(this, step.routine, week.index + 1, day + 1)
}
