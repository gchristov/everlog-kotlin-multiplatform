package com.everlog.android.plan.data

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.model.ELPlanDay
import org.apache.commons.lang3.SerializationUtils

/**
 * The plan week the user is on: the one holding the next day to do, or the last week once every day is done.
 *
 * @param index The week's position in the plan, from 0
 * @param nextDay The position of the next day to do within [days], or null once the plan is done
 */
data class CurrentPlanWeek(
    val index: Int,
    val weekCount: Int,
    val days: List<ELPlanDay>,
    val nextDay: Int?,
)

// What starting the plan's next day does
sealed interface PlanNextStep {
    data class Workout(val routine: ELRoutine) : PlanNextStep

    // A rest day, or one with nothing set, which starting simply marks done
    data object RestDay : PlanNextStep

    // Every day is done
    data object Finish : PlanNextStep
}

fun ELPlan.currentWeek(): CurrentPlanWeek? {
    if (weeks.isEmpty()) return null
    val next = getDayIndex()
    val done = next >= getDaysCount()
    val index = if (done) weeks.lastIndex else next / DaysPerWeek
    return CurrentPlanWeek(index, weeks.size, weeks[index].getDays(), if (done) null else next % DaysPerWeek)
}

fun ELPlan.nextStep(): PlanNextStep {
    val day = getNextDay() ?: return PlanNextStep.Finish
    return day.getRoutine()?.let { PlanNextStep.Workout(it) } ?: PlanNextStep.RestDay
}

// A copy with the next day turned into a rest day and done, or the plan itself when every day is done
fun ELPlan.withNextDaySkipped(): ELPlan = withNextDay {
    setRest(true)
    complete = true
}

// A copy with the next day, a rest day or one with nothing set, done
fun ELPlan.withNextRestDayDone(): ELPlan = withNextDay {
    complete = true
    next = false
}

private fun ELPlan.withNextDay(change: ELPlanDay.() -> Unit): ELPlan {
    if (getNextDay() == null) return this
    val copy = SerializationUtils.clone(this)
    copy.getNextDay()!!.change()
    return copy
}

private const val DaysPerWeek = 7
