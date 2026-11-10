package com.everlog.android.home.data.workouts

import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.exercise.testfixtures.FakeExerciseRepository
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.testfixtures.FakePlanRepository
import com.everlog.android.template.testfixtures.FakeRoutineRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class RealLoadWorkoutsTabListsUseCaseTest {

    private val routines = FakeRoutineRepository()
    private val plans = FakePlanRepository()
    private val useCase = RealLoadWorkoutsTabListsUseCase(FakeCoroutineDispatcher, routines, plans, FakeExerciseRepository())

    private fun plan(name: String, vararg days: String) = ELPlan.newPlan("user").apply {
        this.name = name
        weeks.single().getDays().forEachIndexed { index, day -> days.getOrNull(index)?.let { day.setRoutine(template(it)) } }
    }

    @Test
    fun `templates and plans by name, the plans with the templates their days use`() = runBlocking<Unit> {
        routines.routines.value = Load.Ready(listOf(template("b", "bench day"), template("a", "Arms"), template("c", "Calves")))
        plans.plans.value = Load.Ready(listOf(plan("Summer", "c"), plan("autumn", "a", "b")))

        val lists = (useCase().first() as Load.Ready).value

        assertThat(lists.templates.map { it.name }).containsExactly("Arms", "bench day", "Calves").inOrder()
        assertThat(lists.plans.map { it.plan.name }).containsExactly("autumn", "Summer").inOrder()
        assertThat(lists.plans.first().routines.keys).containsExactly("a", "b")
    }

    @Test
    fun `waits for both`() = runBlocking<Unit> {
        routines.routines.value = Load.Ready(emptyList())

        assertThat(useCase().first()).isEqualTo(Load.Loading)
    }

    @Test
    fun `unavailable when either is`() = runBlocking<Unit> {
        routines.routines.value = Load.Ready(emptyList())
        plans.plans.value = Load.Unavailable(IllegalStateException("offline"))

        assertThat(useCase().first()).isInstanceOf(Load.Unavailable::class.java)
    }
}
