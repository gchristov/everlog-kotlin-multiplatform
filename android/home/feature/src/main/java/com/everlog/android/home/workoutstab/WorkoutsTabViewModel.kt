package com.everlog.android.home.workoutstab

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.mvvm.CommonViewModel
import com.everlog.android.home.HomeAnalytics
import com.everlog.android.home.data.workouts.LoadTemplateHistoryUseCase
import com.everlog.android.home.data.workouts.LoadWorkoutsTabListsUseCase
import com.everlog.android.home.data.workouts.TemplateHistory
import com.everlog.android.home.data.workouts.WorkoutsTabCard
import com.everlog.android.home.data.workouts.WorkoutsTabLists
import com.everlog.android.home.data.workouts.workoutsTabCard
import com.everlog.android.plan.data.PlanProgress
import com.everlog.android.plan.data.model.ELPlan
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.threeten.bp.LocalDate

/**
 * Home's Workouts tab: the user's templates and plans, and at most one card on top saying what to do next (the
 * plan's next day, a suggestion from their history, where to start, or how to get started). The ongoing plan and
 * the date are read again on [onShown], when the tab shows.
 */
class WorkoutsTabViewModel(
    dispatcher: CoroutineDispatcher,
    private val lists: LoadWorkoutsTabListsUseCase,
    private val history: LoadTemplateHistoryUseCase,
    private val planProgress: PlanProgress,
    private val analytics: HomeAnalytics,
    private val today: () -> LocalDate,
) : CommonViewModel<WorkoutsTabState>(dispatcher, WorkoutsTabState(today = today())) {

    private val ongoingPlan = MutableStateFlow(ongoingPlan())
    private var reading: Job? = null
    // Home builds its tabs before they show, so the card counts as shown once per visit, while the tab is showing
    private var showing = false
    private var cardLogged = false

    init {
        read()
    }

    fun onShown() {
        showing = true
        cardLogged = false
        setState { copy(today = today()) }
        ongoingPlan.value = ongoingPlan()
        logCard()
    }

    fun onHidden() {
        showing = false
    }

    fun onRetry() {
        setState { copy(loading = true, unavailable = false) }
        read()
    }

    fun onTemplateToggled(uuid: String) {
        setState { copy(openTemplates = if (uuid in openTemplates) openTemplates - uuid else openTemplates + uuid) }
    }

    // What the card's Start opens, for the screen to open it
    fun onCardStart(): WorkoutsTabStart? {
        val card = state.value.card
        val start = when (card) {
            is WorkoutsTabCard.PlanNext -> WorkoutsTabStart(card.routine, fromPlan = true)
            is WorkoutsTabCard.Suggested -> WorkoutsTabStart(card.template, fromPlan = false)
            is WorkoutsTabCard.StartHere -> WorkoutsTabStart(card.template, fromPlan = false)
            WorkoutsTabCard.GetStarted, null -> null
        } ?: return null
        analytics.workoutsTabStarted(FromCard, card?.kind, suggested = card is WorkoutsTabCard.Suggested)
        return start
    }

    fun onTemplateStart(template: ELRoutine): WorkoutsTabStart {
        val card = state.value.card
        val suggested = card is WorkoutsTabCard.Suggested && card.template.uuid == template.uuid
        analytics.workoutsTabStarted(FromTemplate, card?.kind, suggested)
        return WorkoutsTabStart(template, fromPlan = false)
    }

    private fun read() {
        reading?.cancel()
        reading = launchCoroutine {
            combine(lists(), history(), ongoingPlan, ::show).collect {}
        }
    }

    private fun show(lists: Load<WorkoutsTabLists>, history: Load<TemplateHistory>, plan: ELPlan?) {
        when (lists) {
            Load.Loading -> Unit
            is Load.Unavailable -> setState { copy(loading = false, unavailable = true) }
            is Load.Ready -> {
                val recent = (history as? Load.Ready)?.value
                val (active, others) = workoutsTabPlans(lists.value.plans, plan)
                setState {
                    copy(
                        loading = false,
                        unavailable = false,
                        card = workoutsTabCard(lists.value.templates, recent, plan),
                        templates = workoutsTabTemplates(lists.value.templates, plan),
                        activePlan = active,
                        plans = others,
                        lastDone = recent?.lastDone,
                        exercises = lists.value.exercises,
                        exampleImages = lists.value.exerciseImages.filterKeys { it in WorkoutsTabExamples.all },
                    )
                }
                logCard()
            }
        }
    }

    private fun logCard() {
        val card = state.value.card ?: return
        if (!showing || cardLogged) return
        cardLogged = true
        analytics.workoutsCardShown(card.kind)
    }

    private fun ongoingPlan(): ELPlan? = if (planProgress.hasOngoingPlan()) planProgress.ongoingPlan() else null

    private companion object {
        const val FromCard = "card"
        const val FromTemplate = "template"
    }
}

// The card's kind, as analytics records it
val WorkoutsTabCard.kind: String
    get() = when (this) {
        is WorkoutsTabCard.PlanNext -> "plan"
        is WorkoutsTabCard.Suggested -> "suggested"
        is WorkoutsTabCard.StartHere -> "start_here"
        WorkoutsTabCard.GetStarted -> "get_started"
    }
