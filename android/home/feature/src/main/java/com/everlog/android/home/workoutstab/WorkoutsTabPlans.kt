package com.everlog.android.home.workoutstab

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.plan.data.PlanNextStep
import com.everlog.android.plan.data.PlanWithRoutines
import com.everlog.android.plan.data.currentWeek
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.nextStep

/**
 * The plan being followed, from the copy on the phone (which knows its progress), and the user's other plans.
 */
fun workoutsTabPlans(plans: List<PlanWithRoutines>, ongoing: ELPlan?): Pair<WorkoutsTabActivePlan?, List<WorkoutsTabPlan>> {
    val active = ongoing?.toActivePlan()
    return active to plans.map { it.plan }.filter { it.uuid != ongoing?.uuid }.map { it.toPlan() }
}

fun workoutsTabTemplates(templates: List<ELRoutine>, ongoing: ELPlan?): List<WorkoutsTabTemplate> {
    val inPlan = ongoing?.getRoutinesToResolve().orEmpty()
    return templates.map { WorkoutsTabTemplate(it, inPlan = it.uuid in inPlan) }
}

private fun ELPlan.toPlan() = WorkoutsTabPlan(
    uuid = uuid.orEmpty(),
    name = name.orEmpty(),
    imageUrl = imageUrl,
    weeks = weeks.size,
    daysPerWeek = weeks.maxOfOrNull { week -> week.getDays().count { it.getRoutine() != null || it.routineUuid != null } } ?: 0,
)

private fun ELPlan.toActivePlan(): WorkoutsTabActivePlan? {
    val week = currentWeek() ?: return null
    val workoutDays = week.days.filter { it.getRoutine() != null || it.routineUuid != null }
    return WorkoutsTabActivePlan(
        plan = toPlan(),
        weekNumber = week.index + 1,
        doneThisWeek = workoutDays.count { it.complete },
        workoutsThisWeek = workoutDays.size,
        next = (nextStep() as? PlanNextStep.Workout)?.routine?.name,
        progress = getProgress(),
    )
}
