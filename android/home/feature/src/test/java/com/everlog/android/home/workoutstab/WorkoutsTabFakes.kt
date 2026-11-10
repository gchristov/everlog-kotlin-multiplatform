package com.everlog.android.home.workoutstab

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.home.HomeAnalytics
import com.everlog.android.home.data.workouts.LoadTemplateHistoryUseCase
import com.everlog.android.home.data.workouts.LoadWorkoutsTabListsUseCase
import com.everlog.android.home.data.workouts.TemplateHistory
import com.everlog.android.home.data.workouts.WorkoutsTabLists
import com.everlog.android.home.data.workouts.templateHistory
import com.everlog.android.plan.data.model.ELPlan
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeLists : LoadWorkoutsTabListsUseCase {
    val lists = MutableStateFlow<Load<WorkoutsTabLists>>(Load.Loading)
    var reads = 0

    override fun invoke() = lists.also { reads++ }
}

internal class FakeHistory : LoadTemplateHistoryUseCase {
    val history = MutableStateFlow<Load<TemplateHistory>>(Load.Loading)

    override fun invoke() = history
}

internal class FakeAnalytics : HomeAnalytics {
    val cardsShown = mutableListOf<String>()
    val starts = mutableListOf<Triple<String, String?, Boolean>>()

    override fun planDayCompleted() = Unit

    override fun planCompleted() = Unit

    override fun notificationShown(title: String?) = Unit

    override fun notificationDismissed(title: String?) = Unit

    override fun notificationTapped(title: String?) = Unit

    override fun workoutsCardShown(kind: String) {
        cardsShown += kind
    }

    override fun workoutsTabStarted(from: String, card: String?, suggested: Boolean) {
        starts += Triple(from, card, suggested)
    }
}

internal fun template(uuid: String, name: String = uuid, createdDate: Long = 0) = ELRoutine(uuid = uuid, name = name, createdDate = createdDate)

// A workout started from each template in turn, a day apart
internal fun history(vararg templates: String): TemplateHistory = templateHistory(
    templates.mapIndexed { day, uuid -> ELWorkout(uuid = "w$day", routine = template(uuid), createdDate = day * 86_400_000L, fromRoutine = true) },
)

// One week: [first] on day one, [second] on day three, rest after; [done] days already done
internal fun plan(uuid: String, first: ELRoutine, second: ELRoutine, done: Int = 0): ELPlan = ELPlan.newPlan("user").apply {
    this.uuid = uuid
    name = "Strength block"
    weeks.single().getDays().forEachIndexed { index, day ->
        when (index) {
            0 -> day.setRoutine(first)
            2 -> day.setRoutine(second)
            else -> day.setRest(true)
        }
        day.complete = index < done
    }
}
