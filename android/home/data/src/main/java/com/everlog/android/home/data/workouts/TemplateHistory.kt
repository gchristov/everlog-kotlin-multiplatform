package com.everlog.android.home.data.workouts

import com.everlog.android.common.data.model.workout.ELWorkout

/**
 * The templates the user's recent workouts were started from.
 *
 * @param order Each workout's template uuid, oldest first. Empty workouts are left out.
 * @param lastDone When each template was last started.
 * @param names Each template's name as of its latest workout, for one deleted since.
 */
data class TemplateHistory(
    val order: List<String>,
    val lastDone: Map<String, Long>,
    val names: Map<String, String>,
)

fun templateHistory(workouts: List<ELWorkout>): TemplateHistory {
    val fromTemplates = workouts
        .filter { it.fromRoutine && it.routine?.uuid != null }
        .sortedBy { it.createdDate }
    return TemplateHistory(
        order = fromTemplates.map { it.routine!!.uuid!! },
        lastDone = fromTemplates.associate { it.routine!!.uuid!! to it.createdDate },
        names = fromTemplates.mapNotNull { workout -> workout.routine!!.name?.let { workout.routine!!.uuid!! to it } }.toMap(),
    )
}
