package com.everlog.android.home.workoutstab

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.home.data.workouts.WorkoutsTabCard
import org.threeten.bp.LocalDate

data class WorkoutsTabState(
    val today: LocalDate,
    val loading: Boolean = true,
    // The lists couldn't load and nothing was on the phone
    val unavailable: Boolean = false,
    val card: WorkoutsTabCard? = null,
    val templates: List<WorkoutsTabTemplate> = emptyList(),
    val activePlan: WorkoutsTabActivePlan? = null,
    val plans: List<WorkoutsTabPlan> = emptyList(),
    val openTemplates: Set<String> = emptySet(),
    // When each template was last started, by uuid; null until the history has loaded
    val lastDone: Map<String, Long>? = null,
    // The library's and the user's own; null until both are read
    val exercises: Int? = null,
)

data class WorkoutsTabTemplate(val routine: ELRoutine, val inPlan: Boolean) {
    val uuid: String get() = routine.uuid.orEmpty()
}

data class WorkoutsTabPlan(val uuid: String, val name: String, val imageUrl: String?, val weeks: Int, val daysPerWeek: Int)

/**
 * The plan being followed, from the one on the phone.
 *
 * @param next The next workout day's template name, null when the next day is a rest day or the plan is done.
 */
data class WorkoutsTabActivePlan(
    val plan: WorkoutsTabPlan,
    val weekNumber: Int,
    val doneThisWeek: Int,
    val workoutsThisWeek: Int,
    val next: String?,
    val progress: Int,
)

// A workout to start from the tab: [fromPlan] counts it as the plan's next day
data class WorkoutsTabStart(val routine: ELRoutine, val fromPlan: Boolean)
