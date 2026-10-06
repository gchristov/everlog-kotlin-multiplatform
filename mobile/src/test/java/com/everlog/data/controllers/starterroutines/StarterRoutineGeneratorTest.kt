package com.everlog.data.controllers.starterroutines

import com.everlog.data.controllers.starterroutines.StarterProfile.Experience
import com.everlog.data.controllers.starterroutines.StarterProfile.Goal
import com.everlog.data.controllers.starterroutines.StarterProfile.Place
import com.everlog.data.controllers.starterroutines.StarterRoutine.Target
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.set.ELSetType
import com.google.common.truth.Truth.assertThat
import org.junit.After
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
import timber.log.Timber

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

    // Exercises chosen, as global exercise uuids

    @Test
    fun `gym full body`() {
        val week = generate(days = 2, place = Place.GYM, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Full body A", listOf(parallelSquat, benchPress, bentOverRow, romanianDeadlift, lateralRaise, elbowPlank),
            "Full body B", listOf(barbellDeadlift, militaryPress, latPulldown, bulgarianSplitSquat, dumbbellInclinePress, cablePushdown),
        ).inOrder()
    }

    @Test
    fun `gym full body when just starting`() {
        val week = generate(days = 3, place = Place.GYM, experience = Experience.JUST_STARTING)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Full body A", listOf(parallelSquat, benchPress, bentOverRow, romanianDeadlift),
            "Full body B", listOf(barbellDeadlift, militaryPress, latPulldown, bulgarianSplitSquat),
            "Full body C", listOf(trapBarDeadlift, inclineBenchPress, oneArmRow, forwardLunge),
        ).inOrder()
    }

    @Test
    fun `gym upper and lower`() {
        val week = generate(days = 4, place = Place.GYM, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Upper", listOf(benchPress, bentOverRow, militaryPress, latPulldown, lateralRaise, cablePushdown),
            "Lower", listOf(parallelSquat, romanianDeadlift, bulgarianSplitSquat, hipThrust, seatedCalfRaise, hangingKneeRaise),
        ).inOrder()
    }

    @Test
    fun `gym push pull legs`() {
        val week = generate(days = 5, place = Place.GYM, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Push", listOf(benchPress, militaryPress, dumbbellInclinePress, lateralRaise, cablePushdown, skullCrusher),
            "Pull", listOf(barbellDeadlift, pullUp, bentOverRow, latPulldown, cableFacePull, ezBarCurl),
            "Legs", listOf(parallelSquat, straightLegDeadlift, forwardLunge, hipThrust, singleLegCalfRaise, hangingKneeRaise),
        ).inOrder()
    }

    @Test
    fun `home full body`() {
        val week = generate(days = 2, place = Place.HOME_DUMBBELLS, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Full body A", listOf(gobletSquat, dumbbellFloorPress, oneArmRow, rearAlternatingLunge, shoulderPress, elbowPlank),
            "Full body B", listOf(sumoSquat, pushUp, chestPullOver, hipThrust, lateralRaise, russianTwist),
        ).inOrder()
    }

    @Test
    fun `home full body when just starting`() {
        val week = generate(days = 3, place = Place.HOME_DUMBBELLS, experience = Experience.JUST_STARTING)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Full body A", listOf(gobletSquat, dumbbellFloorPress, oneArmRow, rearAlternatingLunge),
            "Full body B", listOf(sumoSquat, pushUp, chestPullOver, hipThrust),
            "Full body C", listOf(bulgarianSplitSquat, arnoldPress, reverseFly, sideSquat),
        ).inOrder()
    }

    @Test
    fun `home upper and lower`() {
        val week = generate(days = 4, place = Place.HOME_DUMBBELLS, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Upper", listOf(dumbbellFloorPress, oneArmRow, shoulderPress, reverseFly, standingCurl, seatedOverheadExtension),
            "Lower", listOf(gobletSquat, rearAlternatingLunge, bulgarianSplitSquat, hipThrust, singleLegCalfRaise, elbowPlank),
        ).inOrder()
    }

    @Test
    fun `home push pull legs`() {
        val week = generate(days = 5, place = Place.HOME_DUMBBELLS, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Push", listOf(dumbbellFloorPress, shoulderPress, pushUp, lateralRaise, seatedOverheadExtension, tricepKickback),
            "Pull", listOf(oneArmRow, chestPullOver, reverseFly, shrug, dumbbellHammerCurl, standingCurl),
            "Legs", listOf(gobletSquat, bulgarianSplitSquat, sumoSquat, hipThrust, singleLegCalfRaise, russianTwist),
        ).inOrder()
    }

    @Test
    fun `bodyweight full body`() {
        val week = generate(days = 2, place = Place.BODYWEIGHT, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Full body A", listOf(squat, pushUp, rearLunge, supermanHold, calfRaise, elbowPlank),
            "Full body B", listOf(forwardLunge, declinePushUp, donkeyKick, benchDip, cobra, bicycleCrunch),
        ).inOrder()
    }

    @Test
    fun `bodyweight full body when just starting`() {
        val week = generate(days = 3, place = Place.BODYWEIGHT, experience = Experience.JUST_STARTING)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Full body A", listOf(squat, pushUp, rearLunge, supermanHold),
            "Full body B", listOf(forwardLunge, declinePushUp, donkeyKick, benchDip),
            "Full body C", listOf(squat, closeGripPushUp, rearLunge, supermanHold),
        ).inOrder()
    }

    @Test
    fun `bodyweight upper and lower`() {
        val week = generate(days = 4, place = Place.BODYWEIGHT, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Upper", listOf(pushUp, supermanHold, declinePushUp, benchDip, cobra, elbowPlank),
            "Lower", listOf(squat, rearLunge, forwardLunge, donkeyKick, calfRaise, sidePlank),
        ).inOrder()
    }

    @Test
    fun `bodyweight push pull legs`() {
        val week = generate(days = 5, place = Place.BODYWEIGHT, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Push", listOf(pushUp, declinePushUp, closeGripPushUp, tigerPushUp, benchDip, elbowPlank),
            "Pull", listOf(supermanHold, cobra, plankReachThrough, sidePlank, vCrunch, bicycleCrunch),
            "Legs", listOf(squat, rearLunge, forwardLunge, donkeyKick, calfRaise, pistolSquat),
        ).inOrder()
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
    fun `a week builds one routine per day type, with a single set group per exercise`() {
        val week = generate(days = 4, goal = Goal.BUILD_MUSCLE)
        val library = library(week)

        val routines = week.toRoutines(library, createdDate = 1_000L)

        assertThat(routines.map { it.name }).containsExactly("Upper", "Lower").inOrder()
        routines.forEach { routine ->
            assertThat(routine.uuid).isNotNull()
            assertThat(routine.createdDate).isEqualTo(1_000L)
            assertThat(routine.exerciseGroups.map { it.type }.distinct()).containsExactly(ELSetType.SINGLE.name)
            assertThat(routine.exerciseGroups.map { it.restTimeSeconds }.distinct()).containsExactly(90)
        }
        val upper = routines.first().exerciseGroups.map { it.exercises.single().exercise }
        assertThat(upper.map { it?.uuid }).containsExactlyElementsIn(week.routines.first().exercises.map { it.exerciseId }).inOrder()
        // The library's own exercise, not a copy
        assertThat(upper.first()).isSameInstanceAs(library.first { it.uuid == benchPress })
    }

    @Test
    fun `routine sets aim for the top of the rep range`() {
        val week = generate(goal = Goal.BUILD_MUSCLE)

        val routines = week.toRoutines(library(week), createdDate = 0L)

        routines.flatMap { it.exerciseGroups }.flatMap { it.exercises }.forEach { exercise ->
            assertThat(exercise.sets).hasSize(3)
            assertThat(exercise.sets.map { it.getRequiredReps() }.distinct()).containsExactly(12)
        }
    }

    @Test
    fun `routine holds are timed sets`() {
        val week = generate(days = 3, place = Place.BODYWEIGHT, experience = Experience.A_WHILE)

        val pull = week.toRoutines(library(week), createdDate = 0L).single { it.name == "Pull" }

        val superman = pull.exerciseGroups.first().exercises.single()
        assertThat(superman.getName()).isEqualTo("Superman Hold")
        assertThat(superman.sets.map { it.getRequiredTimeSeconds() to it.getRequiredReps() }.distinct()).containsExactly(30 to 0)
    }

    @Test
    fun `no routines are built when exercises are missing from the library, and they are reported`() {
        val week = generate(days = 4)
        val library = library(week).filterNot { it.uuid == benchPress || it.uuid == parallelSquat }
        val errors = recordErrors()

        val thrown = assertThrows(StarterWeek.ExerciseNotFoundException::class.java) {
            week.toRoutines(library, createdDate = 0L)
        }

        assertThat(thrown).hasMessageThat().isEqualTo(
            "Not in the exercise library: Upper Bench Press ($benchPress), Lower Parallel Squat ($parallelSquat)"
        )
        assertThat(errors).containsExactly(thrown)
    }

    @Test
    fun `a week with every exercise in the library reports nothing`() {
        val week = generate(days = 5, experience = Experience.YEARS)
        val errors = recordErrors()

        week.toRoutines(library(week), createdDate = 0L)

        assertThat(errors).isEmpty()
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    // Helpers

    // Errors logged through Timber, which the app sends to Crashlytics as non-fatals
    private fun recordErrors(): List<Throwable> {
        val errors = mutableListOf<Throwable>()
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (t != null) errors += t
            }
        })
        return errors
    }

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

    private fun exerciseIdsByRoutine(week: StarterWeek) = week.routines.associate { it.name to it.exercises.map { exercise -> exercise.exerciseId } }

    // The week's exercises as they'd come from the exercise store
    private fun library(week: StarterWeek) = week.routines.flatMap { it.exercises }
        .distinctBy { it.exerciseId }
        .map { ELExercise(uuid = it.exerciseId, name = it.name) }

    private companion object {
        // Exercises in global/exercises/all, the same in dev and prod
        const val arnoldPress = "ed1bfc44-bd75-4d12-8e41-2740b010c54c" // Arnold Press
        const val barbellDeadlift = "fccec58d-97db-4d3c-9584-41a71ddbe72c" // Barbell Deadlift
        const val benchDip = "f97f7709-729b-4863-b531-1aac552e6a7d" // Bench Dip
        const val benchPress = "a30b9d26-9079-41ed-a3d4-9f1c309f200e" // Bench Press
        const val bentOverRow = "16112844-fe70-48d3-9738-9dd2b9245fa9" // Bent Over Row
        const val bicycleCrunch = "2b1d7c68-15f5-44a3-9f25-0b69b7cb97d6" // Bicycle Crunch
        const val bulgarianSplitSquat = "c1cd66bd-db95-46b4-93cb-70944bcbee97" // Bulgarian Split Squat
        const val cableFacePull = "2851b948-ebd6-4ca2-8bd8-84f5a0b8b637" // Cable Face Pull
        const val cablePushdown = "c8ba67c1-298b-42aa-a481-66ae072ee857" // Cable Pushdown
        const val calfRaise = "613da756-3874-4669-98ca-a6204ec34ecf" // Calf Raise
        const val chestPullOver = "dd0931e6-2370-4b1b-a53d-36e28814f573" // Chest Pull Over
        const val closeGripPushUp = "a6c1a5e6-efae-4065-b8ab-78cb2e60263b" // Close Grip Push Up
        const val cobra = "7a9a92a9-2d7d-418f-addd-722af5dd7e79" // Cobra
        const val declinePushUp = "11de1ab8-e2de-4213-af98-991316042fcb" // Decline Push Up
        const val donkeyKick = "7d992f3c-014b-4cb2-ad0e-8046e16431a0" // Donkey Kick
        const val dumbbellFloorPress = "afdba2b4-941b-4bf4-99db-06da1370744d" // Dumbbell Floor Press
        const val dumbbellHammerCurl = "02ad17ce-0398-4038-bd2a-2f4b19446083" // Dumbbell Hammer Curl
        const val dumbbellInclinePress = "b26385f3-9254-439e-b69f-f4eec8157843" // Dumbbell Incline Press
        const val elbowPlank = "f4a0dafb-3fbd-44a4-9917-ff92e4667cef" // Elbow Plank
        const val ezBarCurl = "44a03415-12b1-4ec3-a567-4bdbdc99256c" // EZ Bar Curl
        const val forwardLunge = "068a5a00-15dc-450e-a0d2-2b6e1c65c983" // Forward Lunge
        const val gobletSquat = "7479a68d-667e-42b4-8c02-6f5c7743de1c" // Goblet Squat
        const val hangingKneeRaise = "1e7736da-08b9-4624-bb70-081c9e813dbc" // Hanging Knee Raise
        const val hipThrust = "3689ed44-836a-404c-831a-7ec1f1ac2374" // Hip Thrust
        const val inclineBenchPress = "773c52e9-d802-4e67-8945-afc363d53a4e" // Incline Bench Press
        const val latPulldown = "da299546-de47-46b8-8829-261e85bf66ef" // Lat Pulldown
        const val lateralRaise = "a273b91b-fad3-4609-975c-d5f2077ebc0e" // Lateral Raise
        const val militaryPress = "a428a2aa-53d9-495b-8ac3-7bb2e9d9425f" // Military Press
        const val oneArmRow = "2c132c78-1c40-412d-b3c3-c8a59fdfc4ee" // One Arm Row
        const val parallelSquat = "1216946b-6b6e-4053-9f7b-c4c482898022" // Parallel Squat
        const val pistolSquat = "08d62221-8ba6-452d-a534-f4187dcf63f1" // Pistol Squat
        const val plankReachThrough = "4a74516d-21d4-4614-8d30-a4201fbd65b5" // Plank Reach Through
        const val pullUp = "dda9a745-6ab4-4db7-948d-ce32fe0f4213" // Pull Up
        const val pushUp = "ae592bb3-22fa-44b1-a584-0020912cdad4" // Push Up
        const val rearAlternatingLunge = "fe36fe89-b99f-46d6-abcc-992bf2c5348c" // Rear Alternating Lunge
        const val rearLunge = "bd817796-b999-42d5-a4fc-359319ad0bdc" // Rear Lunge
        const val reverseFly = "93729477-7e69-4f22-8e0b-27f204aeab58" // Reverse Fly
        const val romanianDeadlift = "7a060e5a-a8d0-46b0-84af-1ecd2671be9d" // Romanian Deadlift
        const val russianTwist = "78402cfa-ef6a-4fc2-82e6-8aa6d008a8e4" // Russian Twist
        const val seatedCalfRaise = "0a5781b5-8f9b-4937-9ef5-341f0f75bbda" // Seated Calf Raise
        const val seatedOverheadExtension = "fedbe2c6-a1cb-4203-97e4-01a1f8aa6a83" // Seated Overhead Extension
        const val shoulderPress = "bc29d6c5-b146-47c5-b5bb-3e41d94d70a0" // Shoulder Press
        const val shrug = "c0d42a30-4dbf-40f0-b207-0232bdf9aa0d" // Shrug
        const val sidePlank = "249f99cc-a714-42af-b26f-70e710f74db8" // Side Plank
        const val sideSquat = "e5bb0eb8-6758-4b88-b0eb-cd79e56a5677" // Side Squat
        const val singleLegCalfRaise = "9e03139a-ee4d-4902-8dcf-d30c73f44a66" // Single Leg Calf Raise
        const val skullCrusher = "fa7ad1c9-7474-4aaf-9f88-185d3f5e4918" // Skull Crusher
        const val squat = "8d2d3fcb-7084-4932-ad62-25c2be2823f9" // Squat
        const val standingCurl = "e922c80e-1a09-444e-b49c-20255eb0cfb1" // Standing Curl
        const val straightLegDeadlift = "4c8d109b-c959-4214-a0de-cdadd9caeb75" // Straight Leg Deadlift
        const val sumoSquat = "6556e3cf-1dc5-4c5a-bd97-d8e52747a590" // Sumo Squat
        const val supermanHold = "6b38fc07-e59f-4a6b-b94f-97880838e9e3" // Superman Hold
        const val tigerPushUp = "92e7eafd-401b-467a-b3b0-d202c9041b53" // Tiger Push Up
        const val trapBarDeadlift = "7b41837e-9300-4cfa-80aa-e1a47b4685ff" // Trap Bar Deadlift
        const val tricepKickback = "ffa01a27-3f09-4ba2-b9bc-fa8404800c8e" // Tricep Kickback
        const val vCrunch = "a4904689-5cc8-454b-b8de-e67e20f8490e" // V Crunch
    }
}
