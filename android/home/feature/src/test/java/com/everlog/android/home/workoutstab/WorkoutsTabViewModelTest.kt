package com.everlog.android.home.workoutstab

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.home.data.workouts.WorkoutsTabCard
import com.everlog.android.home.data.workouts.WorkoutsTabLists
import com.everlog.android.plan.testfixtures.FakePlanProgress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.LocalDate

class WorkoutsTabViewModelTest {

    private val push = template("push", "Push day", createdDate = 2)
    private val pull = template("pull", "Pull day", createdDate = 1)
    private val legs = template("legs", "Legs", createdDate = 3)
    private val rotation = history("push", "pull", "legs", "push", "pull", "legs", "push", "pull", "legs", "push")

    private val lists = FakeLists()
    private val recent = FakeHistory()
    private val planProgress = FakePlanProgress()
    private val analytics = FakeAnalytics()
    private var today = LocalDate.of(2026, 11, 10)

    private fun viewModel() = WorkoutsTabViewModel(FakeCoroutineDispatcher, lists, recent, planProgress, analytics) { today }

    private fun ready(vararg templates: ELRoutine) {
        lists.lists.value = Load.Ready(WorkoutsTabLists(templates.toList(), emptyList()))
    }

    // Main journeys

    @Test
    fun `loading until the templates and plans are there`() {
        val viewModel = viewModel()

        assertThat(viewModel.state.value.loading).isTrue()

        ready(push, pull, legs)

        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.templates.map { it.uuid }).containsExactly("push", "pull", "legs").inOrder()
    }

    @Test
    fun `a clear pattern suggests the next template, and Start starts it`() {
        val viewModel = viewModel()
        ready(push, pull, legs)
        recent.history.value = Load.Ready(rotation)

        assertThat(viewModel.state.value.card).isEqualTo(WorkoutsTabCard.Suggested(pull, afterName = "Push day", times = 3, of = 3))
        assertThat(viewModel.onCardStart()).isEqualTo(WorkoutsTabStart(pull, fromPlan = false))
        assertThat(analytics.starts).containsExactly(Triple("card", "suggested", true))
    }

    @Test
    fun `the plan's next workout comes first and starts from the plan`() {
        planProgress.plan = plan("strength", push, legs, done = 0)
        val viewModel = viewModel()
        ready(push, pull, legs)
        recent.history.value = Load.Ready(rotation)

        assertThat(viewModel.state.value.card).isInstanceOf(WorkoutsTabCard.PlanNext::class.java)
        assertThat(viewModel.onCardStart()).isEqualTo(WorkoutsTabStart(push, fromPlan = true))
        assertThat(viewModel.state.value.activePlan?.plan?.uuid).isEqualTo("strength")
    }

    @Test
    fun `starting a template from the list says whether it was the suggested one`() {
        val viewModel = viewModel()
        ready(push, pull, legs)
        recent.history.value = Load.Ready(rotation)

        assertThat(viewModel.onTemplateStart(pull)).isEqualTo(WorkoutsTabStart(pull, fromPlan = false))
        viewModel.onTemplateStart(legs)

        assertThat(analytics.starts).containsExactly(
            Triple("template", "suggested", true),
            Triple("template", "suggested", false),
        ).inOrder()
    }

    // Options

    @Test
    fun `templates open and close`() {
        val viewModel = viewModel()
        ready(push, pull)

        viewModel.onTemplateToggled("push")
        assertThat(viewModel.state.value.openTemplates).containsExactly("push")

        viewModel.onTemplateToggled("pull")
        viewModel.onTemplateToggled("push")
        assertThat(viewModel.state.value.openTemplates).containsExactly("pull")
    }

    @Test
    fun `templates but no recent workouts start with the one saved first`() {
        val viewModel = viewModel()
        ready(push, pull, legs)
        recent.history.value = Load.Ready(history())

        assertThat(viewModel.state.value.card).isEqualTo(WorkoutsTabCard.StartHere(pull))
    }

    @Test
    fun `no templates means getting started, which starts nothing itself`() {
        val viewModel = viewModel()
        ready()

        assertThat(viewModel.state.value.card).isEqualTo(WorkoutsTabCard.GetStarted)
        assertThat(viewModel.onCardStart()).isNull()
        assertThat(analytics.starts).isEmpty()
    }

    @Test
    fun `when each template was last done comes with the history`() {
        val viewModel = viewModel()
        ready(push, pull, legs)
        assertThat(viewModel.state.value.lastDone).isNull()

        recent.history.value = Load.Ready(rotation)

        assertThat(viewModel.state.value.lastDone).containsKey("push")
    }

    // Edge cases

    @Test
    fun `the card arrives once the history does, after the lists`() {
        val viewModel = viewModel()
        ready(push, pull, legs)
        assertThat(viewModel.state.value.card).isNull()

        recent.history.value = Load.Ready(rotation)

        assertThat(viewModel.state.value.card).isNotNull()
    }

    @Test
    fun `history that can't load leaves no suggestion, but the lists show`() {
        val viewModel = viewModel()
        ready(push, pull, legs)
        recent.history.value = Load.Unavailable(IllegalStateException("offline"))

        assertThat(viewModel.state.value.card).isNull()
        assertThat(viewModel.state.value.templates).hasSize(3)
    }

    @Test
    fun `lists that can't load say so, and trying again reads them again`() {
        val viewModel = viewModel()
        lists.lists.value = Load.Unavailable(IllegalStateException("offline"))
        assertThat(viewModel.state.value.unavailable).isTrue()

        viewModel.onRetry()
        ready(push)

        assertThat(lists.reads).isEqualTo(2)
        assertThat(viewModel.state.value.unavailable).isFalse()
        assertThat(viewModel.state.value.templates).hasSize(1)
    }

    @Test
    fun `a plan started elsewhere shows when the tab does`() {
        val viewModel = viewModel()
        ready(push, pull, legs)

        planProgress.plan = plan("strength", push, legs)
        viewModel.onShown()

        assertThat(viewModel.state.value.activePlan).isNotNull()
        assertThat(viewModel.state.value.card).isInstanceOf(WorkoutsTabCard.PlanNext::class.java)
    }

    @Test
    fun `the date moves on when the tab shows again`() {
        val viewModel = viewModel()
        today = today.plusDays(1)

        viewModel.onShown()

        assertThat(viewModel.state.value.today).isEqualTo(LocalDate.of(2026, 11, 11))
    }

    // Analytics

    @Test
    fun `the card counts as shown once per visit, only while the tab shows`() {
        val viewModel = viewModel()
        ready(push, pull, legs)
        recent.history.value = Load.Ready(rotation)
        assertThat(analytics.cardsShown).isEmpty()

        viewModel.onShown()
        viewModel.onTemplateToggled("push")
        assertThat(analytics.cardsShown).containsExactly("suggested")

        viewModel.onHidden()
        viewModel.onShown()
        assertThat(analytics.cardsShown).containsExactly("suggested", "suggested")
    }

    @Test
    fun `a card that arrives while the tab shows counts then`() {
        val viewModel = viewModel()
        viewModel.onShown()
        ready(push, pull, legs)
        assertThat(analytics.cardsShown).isEmpty()

        recent.history.value = Load.Ready(rotation)

        assertThat(analytics.cardsShown).containsExactly("suggested")
    }
}
