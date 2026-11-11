package com.everlog.android.home.data.workouts

import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.exercise.data.model.library.LibraryExercise
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
    private val exercises = FakeExerciseRepository()
    private val useCase = RealLoadWorkoutsTabListsUseCase(FakeCoroutineDispatcher, routines, plans, exercises)

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
    fun `counts the library's exercises and the user's own`() = runBlocking<Unit> {
        routines.routines.value = Load.Ready(emptyList())
        plans.plans.value = Load.Ready(emptyList())
        exercises.library = Load.Ready(List(3) { LibraryExercise(uuid = "library-$it", name = "Library $it") })
        exercises.own = Load.Ready(listOf(LibraryExercise(uuid = "own", name = "Own", own = true)))

        assertThat((useCase().first() as Load.Ready).value.exercises).isEqualTo(4)
    }

    @Test
    fun `each exercise's image by uuid, for those that have one`() = runBlocking<Unit> {
        routines.routines.value = Load.Ready(emptyList())
        plans.plans.value = Load.Ready(emptyList())
        exercises.library = Load.Ready(listOf(LibraryExercise(uuid = "bench", name = "Bench", imageUrl = "bench.png"), LibraryExercise(uuid = "plank", name = "Plank")))
        exercises.own = Load.Ready(listOf(LibraryExercise(uuid = "own", name = "Own", imageUrl = "own.png", own = true)))

        assertThat((useCase().first() as Load.Ready).value.exerciseImages).containsExactly("bench", "bench.png", "own", "own.png")
    }

    @Test
    fun `no count while either can't be read`() = runBlocking<Unit> {
        routines.routines.value = Load.Ready(emptyList())
        plans.plans.value = Load.Ready(emptyList())
        exercises.library = Load.Unavailable(IllegalStateException("offline"))
        exercises.own = Load.Ready(listOf(LibraryExercise(uuid = "own", name = "Own", own = true)))

        assertThat((useCase().first() as Load.Ready).value.exercises).isNull()
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
