package com.everlog.managers

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
    fun `a workout without a uuid is never the ongoing one`() {
        WorkoutManager.manager.setOngoingWorkout(ELWorkout())

        assertThat(WorkoutManager.manager.isOngoingWorkout(ELWorkout())).isFalse()
    }
}
