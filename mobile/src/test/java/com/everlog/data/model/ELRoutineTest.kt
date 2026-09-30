package com.everlog.data.model

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Test
import java.io.File

class ELRoutineTest {

    // Parsed the same way as CreateSampleRoutinesAsyncTask, which creates these for new accounts.
    // Unit tests run from the module directory.
    private fun sampleRoutines(): Collection<ELRoutine> {
        val json = File("src/main/assets/sample_routines.json").readText()
        val type = object : TypeToken<Map<String, ELRoutine>>() {}.type
        return Gson().fromJson<Map<String, ELRoutine>>(json, type).values
    }

    @Test
    fun `every bundled sample routine is a sample`() {
        val routines = sampleRoutines()

        assertThat(routines).isNotEmpty()
        routines.forEach { assertThat(it.isSample()).isTrue() }
    }

    @Test
    fun `new routines and workouts are not samples`() {
        assertThat(ELRoutine.buildNewRoutine("user").isSample()).isFalse()
        assertThat(ELRoutine.buildEmptyWorkout().isSample()).isFalse()
    }
}
