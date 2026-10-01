package com.everlog.managers

import com.everlog.data.model.ELRoutine
import com.everlog.data.model.workout.ELWorkout
import com.everlog.testutil.InMemorySharedPreferences
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class WorkoutManagerTest {

    @Before
    fun setUp() {
        InMemorySharedPreferences.install()
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    @Test
    fun `a resumed workout is the ongoing one`() {
        WorkoutManager.manager.setOngoingWorkout(ELWorkout(uuid = "ongoing"))

        // Resuming passes a copy of the saved workout
        assertThat(WorkoutManager.manager.isOngoingWorkout(ELWorkout(uuid = "ongoing"))).isTrue()
    }

    @Test
    fun `a new workout isn't the ongoing one while another is saved`() {
        WorkoutManager.manager.setOngoingWorkout(ELWorkout(uuid = "ongoing"))

        assertThat(WorkoutManager.manager.isOngoingWorkout(ELWorkout(uuid = "new"))).isFalse()
    }

    @Test
    fun `a new workout isn't the ongoing one when none is saved`() {
        assertThat(WorkoutManager.manager.isOngoingWorkout(ELWorkout(uuid = "new"))).isFalse()
    }

    @Test
    fun `a finished or stopped workout isn't the ongoing one`() {
        val workout = ELWorkout(uuid = "ongoing")
        WorkoutManager.manager.setOngoingWorkout(workout)

        // As done by finishing or stopping the workout
        WorkoutManager.manager.clearOngoingWorkout()

        assertThat(WorkoutManager.manager.isOngoingWorkout(workout)).isFalse()
    }

    @Test
    fun `the saved copy has what was done since the workout started`() {
        // As when Android rebuilds the workout screen from its launch intent after the app was killed
        val launched = ELWorkout(uuid = "ongoing", routine = ELRoutine())
        val saved = ELWorkout(uuid = "ongoing", routine = ELRoutine(name = "Push day"))
        WorkoutManager.manager.setOngoingWorkout(saved)

        assertThat(WorkoutManager.manager.savedCopyOf(launched)?.routine?.name).isEqualTo("Push day")
    }

    @Test
    fun `a new workout has no saved copy while another one is saved`() {
        WorkoutManager.manager.setOngoingWorkout(ELWorkout(uuid = "ongoing"))

        assertThat(WorkoutManager.manager.savedCopyOf(ELWorkout(uuid = "new"))).isNull()
    }

    @Test
    fun `a workout without a uuid is never the ongoing one`() {
        WorkoutManager.manager.setOngoingWorkout(ELWorkout())

        assertThat(WorkoutManager.manager.isOngoingWorkout(ELWorkout())).isFalse()
    }
}
