package com.everlog.data.controllers.starterroutines

import com.everlog.data.controllers.starterroutines.StarterProfile.Experience
import com.everlog.data.controllers.starterroutines.StarterProfile.Goal
import com.everlog.data.controllers.starterroutines.StarterProfile.Place
import com.everlog.data.controllers.starterroutines.StarterRoutine.Target
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.set.ELSetType
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.DayOfWeek.FRIDAY
import org.threeten.bp.DayOfWeek.MONDAY
import org.threeten.bp.DayOfWeek.SATURDAY
import org.threeten.bp.DayOfWeek.SUNDAY
import org.threeten.bp.DayOfWeek.THURSDAY
import org.threeten.bp.DayOfWeek.TUESDAY
import org.threeten.bp.DayOfWeek.WEDNESDAY

class StarterRoutineGeneratorTest {

    // Split

    @Test
    fun `1 day is a single full body routine`() {
        val week = generate(days = 1)

        assertThat(week.split).isEqualTo(StarterWeek.Split.FULL_BODY)
        assertThat(names(week)).containsExactly("Full body")
    }

    @Test
    fun `2 days alternate two full body routines`() {
        val week = generate(days = 2)

        assertThat(week.split).isEqualTo(StarterWeek.Split.FULL_BODY)
        assertThat(names(week)).containsExactly("Full body A", "Full body B").inOrder()
    }

    @Test
    fun `3 days are three full body routines when just starting`() {
        val week = generate(days = 3, experience = Experience.JUST_STARTING)

        assertThat(week.split).isEqualTo(StarterWeek.Split.FULL_BODY)
        assertThat(names(week)).containsExactly("Full body A", "Full body B", "Full body C").inOrder()
    }

    @Test
    fun `3 days are push pull legs with more experience`() {
        Experience.entries.filter { it != Experience.JUST_STARTING }.forEach { experience ->
            val week = generate(days = 3, experience = experience)

            assertThat(week.split).isEqualTo(StarterWeek.Split.PUSH_PULL_LEGS)
            assertThat(names(week)).containsExactly("Push", "Pull", "Legs").inOrder()
        }
    }

    @Test
    fun `4 days are upper and lower`() {
        val week = generate(days = 4)

        assertThat(week.split).isEqualTo(StarterWeek.Split.UPPER_LOWER)
        assertThat(names(week)).containsExactly("Upper", "Lower").inOrder()
    }

    @Test
    fun `5 to 7 days are push pull legs`() {
        (5..7).forEach { days ->
            val week = generate(days = days, experience = Experience.JUST_STARTING)

            assertThat(week.split).isEqualTo(StarterWeek.Split.PUSH_PULL_LEGS)
            assertThat(names(week)).containsExactly("Push", "Pull", "Legs").inOrder()
        }
    }

    @Test
    fun `days a week outside 1 to 7 are rejected`() {
        listOf(0, 8).forEach { days ->
            assertThrows(IllegalArgumentException::class.java) { generate(days = days) }
        }
    }

    // Days

    @Test
    fun `each training day has one routine`() {
        (1..7).forEach { days ->
            Experience.entries.forEach { experience ->
                val week = generate(days = days, experience = experience)

                val scheduled = week.routines.flatMap { it.days }
                assertThat(scheduled).containsExactlyElementsIn(StarterRoutineGenerator.trainingDays(days))
                assertThat(scheduled).hasSize(days)
            }
        }
    }

    @Test
    fun `upper and lower alternate across 4 days`() {
        val week = generate(days = 4)

        assertThat(days(week)).containsExactly(
            "Upper", listOf(MONDAY, THURSDAY),
            "Lower", listOf(TUESDAY, FRIDAY),
        )
    }

    @Test
    fun `push pull legs repeats from the fourth day`() {
        val week = generate(days = 7)

        assertThat(days(week)).containsExactly(
            "Push", listOf(MONDAY, THURSDAY, SUNDAY),
            "Pull", listOf(TUESDAY, FRIDAY),
            "Legs", listOf(WEDNESDAY, SATURDAY),
        )
    }

    // Exercises

    @Test
    fun `experience sets the number of exercises a day`() {
        mapOf(Experience.JUST_STARTING to 4, Experience.A_WHILE to 5, Experience.YEARS to 6).forEach { (experience, count) ->
            Place.entries.forEach { place ->
                generate(days = 3, place = place, experience = experience).routines.forEach {
                    assertThat(it.exercises).hasSize(count)
                }
            }
        }
    }

    @Test
    fun `no routine repeats an exercise`() {
        everyWeek().flatMap { it.routines }.forEach { routine ->
            val ids = routine.exercises.map { it.exerciseId }
            assertThat(ids).containsNoDuplicates()
        }
    }

    @Test
    fun `places use different exercises`() {
        val gym = exerciseIds(generate(days = 4, place = Place.GYM))
        val home = exerciseIds(generate(days = 4, place = Place.HOME_DUMBBELLS))
        val bodyweight = exerciseIds(generate(days = 4, place = Place.BODYWEIGHT))

        assertThat(gym).isNotEqualTo(home)
        assertThat(home).isNotEqualTo(bodyweight)
        assertThat(gym).isNotEqualTo(bodyweight)
    }

    // Sets and reps

    @Test
    fun `get stronger is heavy on the main lifts`() {
        val routine = generate(goal = Goal.GET_STRONGER, experience = Experience.YEARS).routines.first()

        assertThat(routine.exercises.take(2).map { it.sets to it.target }).containsExactly(
            4 to Target.Reps(4..6),
            4 to Target.Reps(4..6),
        )
        routine.exercises.drop(2).filter { it.target is Target.Reps }.forEach {
            assertThat(it.sets to it.target).isEqualTo(3 to Target.Reps(6..8))
        }
    }

    @Test
    fun `other goals use the same reps for every lift`() {
        mapOf(Goal.BUILD_MUSCLE to 8..12, Goal.BUILD_ENDURANCE to 15..20, Goal.STAY_FIT to 10..12).forEach { (goal, reps) ->
            val exercises = generate(goal = goal, experience = Experience.YEARS).routines.flatMap { it.exercises }

            exercises.filter { it.target is Target.Reps }.forEach {
                assertThat(it.sets to it.target).isEqualTo(3 to Target.Reps(reps))
            }
        }
    }

    @Test
    fun `holds are timed, longer for endurance`() {
        // Bodyweight Pull starts with Superman Hold and Cobra, the main lift slots
        mapOf(Goal.GET_STRONGER to 30, Goal.BUILD_MUSCLE to 30, Goal.BUILD_ENDURANCE to 45, Goal.STAY_FIT to 30).forEach { (goal, seconds) ->
            val pull = generate(days = 3, place = Place.BODYWEIGHT, experience = Experience.A_WHILE, goal = goal)
                .routines.single { it.name == "Pull" }

            assertThat(pull.exercises.take(2).map { it.sets to it.target }).containsExactly(
                3 to Target.Time(seconds),
                3 to Target.Time(seconds),
            )
        }
    }

    @Test
    fun `goal sets the rest time`() {
        mapOf(Goal.GET_STRONGER to 180, Goal.BUILD_MUSCLE to 90, Goal.BUILD_ENDURANCE to 45, Goal.STAY_FIT to 60).forEach { (goal, seconds) ->
            generate(goal = goal).routines.forEach {
                assertThat(it.restTimeSeconds).isEqualTo(seconds)
            }
        }
    }

    // Routines

    @Test
    fun `a routine has a single set group per exercise from the library`() {
        val starter = generate(days = 4, goal = Goal.BUILD_MUSCLE).routines.first()
        val library = library(starter)

        val routine = starter.toRoutine(library, createdDate = 1_000L)

        assertThat(routine.uuid).isNotNull()
        assertThat(routine.name).isEqualTo("Upper")
        assertThat(routine.createdDate).isEqualTo(1_000L)
        assertThat(routine.exerciseGroups.map { it.type }.distinct()).containsExactly(ELSetType.SINGLE.name)
        assertThat(routine.exerciseGroups.map { it.restTimeSeconds }.distinct()).containsExactly(90)
        assertThat(routine.exerciseGroups.map { it.exercises.single().exercise }).containsExactlyElementsIn(library).inOrder()
    }

    @Test
    fun `routine sets aim for the top of the rep range`() {
        val starter = generate(goal = Goal.BUILD_MUSCLE).routines.first()

        val routine = starter.toRoutine(library(starter), createdDate = 0L)

        routine.exerciseGroups.flatMap { it.exercises }.forEach { exercise ->
            assertThat(exercise.sets).hasSize(3)
            assertThat(exercise.sets.map { it.getRequiredReps() }.distinct()).containsExactly(12)
        }
    }

    @Test
    fun `routine holds are timed sets`() {
        val starter = generate(days = 3, place = Place.BODYWEIGHT, experience = Experience.A_WHILE)
            .routines.single { it.name == "Pull" }

        val routine = starter.toRoutine(library(starter), createdDate = 0L)

        val superman = routine.exerciseGroups.first().exercises.single()
        assertThat(superman.getName()).isEqualTo("Superman Hold")
        assertThat(superman.sets.map { it.getRequiredTimeSeconds() to it.getRequiredReps() }.distinct()).containsExactly(30 to 0)
    }

    @Test
    fun `exercises missing from the library are left out`() {
        val week = generate(days = 1)
        val starter = week.routines.single()
        val missing = starter.exercises.first()
        val library = library(starter).filterNot { it.uuid == missing.exerciseId }

        val routine = starter.toRoutine(library, createdDate = 0L)

        assertThat(routine.exerciseGroups).hasSize(starter.exercises.size - 1)
        assertThat(week.missingExercises(library)).containsExactly(missing)
        assertThat(week.missingExercises(library(starter))).isEmpty()
    }

    // Helpers

    private fun generate(
        days: Int = 3,
        place: Place = Place.GYM,
        experience: Experience = Experience.JUST_STARTING,
        goal: Goal = Goal.STAY_FIT,
    ) = StarterRoutineGenerator.generate(StarterProfile(days, place, experience, goal))

    private fun everyWeek() = (1..7).flatMap { days ->
        Place.entries.flatMap { place ->
            Experience.entries.map { experience -> generate(days, place, experience) }
        }
    }

    private fun names(week: StarterWeek) = week.routines.map { it.name }

    private fun days(week: StarterWeek): Map<String, List<DayOfWeek>> = week.routines.associate { it.name to it.days }

    private fun exerciseIds(week: StarterWeek) = week.routines.flatMap { it.exercises }.map { it.exerciseId }.toSet()

    // The routine's exercises as they'd come from the exercise store
    private fun library(routine: StarterRoutine) = routine.exercises.map { ELExercise(uuid = it.exerciseId, name = it.name) }
}
