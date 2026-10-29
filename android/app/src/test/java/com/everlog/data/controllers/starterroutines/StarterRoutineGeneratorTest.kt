package com.everlog.data.controllers.starterroutines

import com.everlog.data.controllers.starterroutines.StarterProfile.Experience
import com.everlog.data.controllers.starterroutines.StarterProfile.Goal
import com.everlog.data.controllers.starterroutines.StarterProfile.Place
import com.everlog.data.controllers.starterroutines.StarterRoutine.Target
import com.everlog.data.model.set.ELSetType
import com.everlog.testutil.exerciseLibrary
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

    // Exercises chosen, as global exercise uuids

    @Test
    fun `gym full body`() {
        val week = generate(days = 2, place = Place.GYM, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Full body A", listOf(barbellSquat, barbellBenchPress, barbellBentOverRow, romanianDeadlift, dumbbellLateralRaise, elbowPlank),
            "Full body B", listOf(barbellDeadlift, militaryPress, latPulldown, legPress, inclineDumbbellBenchPress, cablePushdown),
        ).inOrder()
    }

    @Test
    fun `gym full body when just starting`() {
        val week = generate(days = 3, place = Place.GYM, experience = Experience.JUST_STARTING)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Full body A", listOf(barbellSquat, barbellBenchPress, barbellBentOverRow, romanianDeadlift),
            "Full body B", listOf(barbellDeadlift, militaryPress, latPulldown, legPress),
            "Full body C", listOf(legPress, machineChestPress, seatedCableRow, seatedLegCurl),
        ).inOrder()
    }

    @Test
    fun `gym upper and lower`() {
        val week = generate(days = 4, place = Place.GYM, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Upper", listOf(barbellBenchPress, barbellBentOverRow, militaryPress, latPulldown, barbellCurl, cablePushdown),
            "Lower", listOf(barbellSquat, romanianDeadlift, legPress, seatedLegCurl, barbellHipThrust, hangingKneeRaise),
        ).inOrder()
    }

    @Test
    fun `gym push pull legs`() {
        val week = generate(days = 5, place = Place.GYM, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Push", listOf(barbellBenchPress, militaryPress, inclineDumbbellBenchPress, dumbbellLateralRaise, cablePushdown, skullCrusher),
            "Pull", listOf(barbellDeadlift, latPulldown, seatedCableRow, cableFacePull, barbellCurl, dumbbellHammerCurl),
            "Legs", listOf(barbellSquat, romanianDeadlift, legPress, legExtension, seatedLegCurl, seatedCalfRaise),
        ).inOrder()
    }

    @Test
    fun `home full body`() {
        val week = generate(days = 2, place = Place.HOME_DUMBBELLS, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Full body A", listOf(gobletSquat, dumbbellFloorPress, oneArmDumbbellRow, dumbbellRomanianDeadlift, seatedDumbbellShoulderPress, elbowPlank),
            "Full body B", listOf(dumbbellReverseLunge, pushUp, dumbbellBentOverRow, dumbbellGluteBridge, dumbbellLateralRaise, deadBug),
        ).inOrder()
    }

    @Test
    fun `home full body when just starting`() {
        val week = generate(days = 3, place = Place.HOME_DUMBBELLS, experience = Experience.JUST_STARTING)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Full body A", listOf(gobletSquat, dumbbellFloorPress, oneArmDumbbellRow, dumbbellRomanianDeadlift),
            "Full body B", listOf(dumbbellReverseLunge, pushUp, dumbbellBentOverRow, dumbbellGluteBridge),
            "Full body C", listOf(dumbbellSumoSquat, arnoldPress, oneArmDumbbellRow, dumbbellRomanianDeadlift),
        ).inOrder()
    }

    @Test
    fun `home upper and lower`() {
        val week = generate(days = 4, place = Place.HOME_DUMBBELLS, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Upper", listOf(dumbbellFloorPress, oneArmDumbbellRow, seatedDumbbellShoulderPress, dumbbellBentOverRow, dumbbellCurl, seatedDumbbellOverheadExtension),
            "Lower", listOf(gobletSquat, dumbbellRomanianDeadlift, dumbbellBulgarianSplitSquat, dumbbellGluteBridge, singleLegDumbbellCalfRaise, elbowPlank),
        ).inOrder()
    }

    @Test
    fun `home push pull legs`() {
        val week = generate(days = 5, place = Place.HOME_DUMBBELLS, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Push", listOf(dumbbellFloorPress, seatedDumbbellShoulderPress, pushUp, dumbbellLateralRaise, seatedDumbbellOverheadExtension, dumbbellKickback),
            "Pull", listOf(oneArmDumbbellRow, dumbbellBentOverRow, bentOverDumbbellRearDeltFly, dumbbellCurl, dumbbellHammerCurl, dumbbellShrug),
            "Legs", listOf(gobletSquat, dumbbellRomanianDeadlift, dumbbellReverseLunge, dumbbellStepUp, singleLegDumbbellCalfRaise, russianTwist),
        ).inOrder()
    }

    @Test
    fun `bodyweight full body`() {
        val week = generate(days = 2, place = Place.BODYWEIGHT, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Full body A", listOf(bodyweightSquat, pushUp, supermanPull, bodyweightGluteBridge, reverseLunge, elbowPlank),
            "Full body B", listOf(reverseLunge, pikePushUp, supermanHold, singleLegBodyweightRomanianDeadlift, closeGripPushUp, deadBug),
        ).inOrder()
    }

    @Test
    fun `bodyweight full body when just starting`() {
        val week = generate(days = 3, place = Place.BODYWEIGHT, experience = Experience.JUST_STARTING)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Full body A", listOf(bodyweightSquat, pushUp, supermanPull, bodyweightGluteBridge),
            "Full body B", listOf(reverseLunge, pikePushUp, supermanHold, singleLegBodyweightRomanianDeadlift),
            "Full body C", listOf(bodyweightSquat, kneelingPushUp, supermanPull, bodyweightGluteBridge),
        ).inOrder()
    }

    @Test
    fun `bodyweight upper and lower`() {
        val week = generate(days = 4, place = Place.BODYWEIGHT, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Upper", listOf(pushUp, supermanPull, pikePushUp, supermanHold, kneelingBodyweightTricepsExtension, elbowPlank),
            "Lower", listOf(bodyweightSquat, bodyweightGluteBridge, reverseLunge, singleLegBodyweightRomanianDeadlift, bodyweightCalfRaise, sidePlank),
        ).inOrder()
    }

    @Test
    fun `bodyweight push pull legs`() {
        val week = generate(days = 5, place = Place.BODYWEIGHT, experience = Experience.YEARS)

        assertThat(exerciseIdsByRoutine(week)).containsExactly(
            "Push", listOf(pushUp, pikePushUp, wideGripPushUp, closeGripPushUp, kneelingBodyweightTricepsExtension, elbowPlank),
            "Pull", listOf(supermanPull, supermanHold, birdDog, reverseCrunch, sidePlank, bicycleCrunch),
            "Legs", listOf(bodyweightSquat, reverseLunge, bodyweightGluteBridge, sideLunge, singleLegBodyweightRomanianDeadlift, bodyweightCalfRaise),
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
        // Bodyweight Pull: Superman Hold is the second main lift, Side Plank the fifth exercise
        mapOf(Goal.GET_STRONGER to 30, Goal.BUILD_MUSCLE to 30, Goal.BUILD_ENDURANCE to 45, Goal.STAY_FIT to 30).forEach { (goal, seconds) ->
            val pull = generate(days = 3, place = Place.BODYWEIGHT, experience = Experience.A_WHILE, goal = goal)
                .routines.single { it.name == "Pull" }

            assertThat(listOf(pull.exercises[1], pull.exercises[4]).map { it.sets to it.target }).containsExactly(
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
        val library = exerciseLibrary(week)

        val routines = week.toRoutines(library, createdDate = 1_000L).getOrNull()!!

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
        assertThat(upper.first()).isSameInstanceAs(library.first { it.uuid == barbellBenchPress })
    }

    @Test
    fun `routine sets aim for the top of the rep range`() {
        val week = generate(goal = Goal.BUILD_MUSCLE)

        val routines = week.toRoutines(exerciseLibrary(week), createdDate = 0L).getOrNull()!!

        routines.flatMap { it.exerciseGroups }.flatMap { it.exercises }.forEach { exercise ->
            assertThat(exercise.sets).hasSize(3)
            assertThat(exercise.sets.map { it.getRequiredReps() }.distinct()).containsExactly(12)
        }
    }

    @Test
    fun `routine holds are timed sets`() {
        val week = generate(days = 3, place = Place.BODYWEIGHT, experience = Experience.A_WHILE)

        val pull = week.toRoutines(exerciseLibrary(week), createdDate = 0L).getOrNull()!!.single { it.name == "Pull" }

        val superman = pull.exerciseGroups[1].exercises.single()
        assertThat(superman.exercise?.uuid).isEqualTo(supermanHold)
        assertThat(superman.sets.map { it.getRequiredTimeSeconds() to it.getRequiredReps() }.distinct()).containsExactly(30 to 0)
    }

    @Test
    fun `no routines are built when exercises are missing from the library`() {
        val week = generate(days = 4)
        val library = exerciseLibrary(week).filterNot { it.uuid == barbellBenchPress || it.uuid == barbellSquat }

        val error = week.toRoutines(library, createdDate = 0L).leftOrNull()

        assertThat(error).hasMessageThat().isEqualTo(
            "Not in the exercise library: Upper $barbellBenchPress, Lower $barbellSquat"
        )
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

    private fun exerciseIdsByRoutine(week: StarterWeek) = week.routines.associate { it.name to it.exercises.map { exercise -> exercise.exerciseId } }

    private companion object {
        // Exercises in global/exercises/all, the same in dev and prod
        const val arnoldPress = "ed1bfc44-bd75-4d12-8e41-2740b010c54c" // Arnold Press
        const val barbellBenchPress = "a30b9d26-9079-41ed-a3d4-9f1c309f200e" // Barbell Bench Press
        const val barbellBentOverRow = "16112844-fe70-48d3-9738-9dd2b9245fa9" // Barbell Bent-Over Row
        const val barbellCurl = "6bb3fc82-b228-5354-aeef-50dc481ab462" // Barbell Curl
        const val barbellDeadlift = "fccec58d-97db-4d3c-9584-41a71ddbe72c" // Barbell Deadlift
        const val barbellHipThrust = "3689ed44-836a-404c-831a-7ec1f1ac2374" // Barbell Hip Thrust
        const val barbellSquat = "8d2d3fcb-7084-4932-ad62-25c2be2823f9" // Barbell Squat
        const val bentOverDumbbellRearDeltFly = "5ebd43ce-c87f-4833-9fbf-6c14ccd8dba8" // Bent-Over Dumbbell Rear Delt Fly
        const val bicycleCrunch = "2b1d7c68-15f5-44a3-9f25-0b69b7cb97d6" // Bicycle Crunch
        const val birdDog = "d652a86a-98f8-5bf2-a1f4-b1b601491599" // Bird Dog
        const val bodyweightCalfRaise = "8c81478e-e98b-5f0e-8ae3-ccd77dd44bcd" // Bodyweight Calf Raise
        const val bodyweightGluteBridge = "5338dea5-7d97-5f32-8f49-6dc0bcdc6b84" // Bodyweight Glute Bridge
        const val bodyweightSquat = "4d92c102-e94a-5f71-bfde-b49938a71e80" // Bodyweight Squat
        const val cableFacePull = "2851b948-ebd6-4ca2-8bd8-84f5a0b8b637" // Cable Face Pull
        const val cablePushdown = "c8ba67c1-298b-42aa-a481-66ae072ee857" // Cable Pushdown
        const val closeGripPushUp = "a6c1a5e6-efae-4065-b8ab-78cb2e60263b" // Close-Grip Push-Up
        const val deadBug = "b6d7db4a-e3cc-58d4-9387-df0f25d4ef44" // Dead Bug
        const val dumbbellBentOverRow = "e4d38a62-3615-59ba-81f5-3ed42b688d7a" // Dumbbell Bent-Over Row
        const val dumbbellBulgarianSplitSquat = "b5410e06-9c17-5f1b-9eea-ef6b3a1adfc8" // Dumbbell Bulgarian Split Squat
        const val dumbbellCurl = "e922c80e-1a09-444e-b49c-20255eb0cfb1" // Dumbbell Curl
        const val dumbbellFloorPress = "afdba2b4-941b-4bf4-99db-06da1370744d" // Dumbbell Floor Press
        const val dumbbellGluteBridge = "89c5c37c-2098-509e-bc14-080f3e7d5e2e" // Dumbbell Glute Bridge
        const val dumbbellHammerCurl = "02ad17ce-0398-4038-bd2a-2f4b19446083" // Dumbbell Hammer Curl
        const val dumbbellKickback = "ffa01a27-3f09-4ba2-b9bc-fa8404800c8e" // Dumbbell Kickback
        const val dumbbellLateralRaise = "a273b91b-fad3-4609-975c-d5f2077ebc0e" // Dumbbell Lateral Raise
        const val dumbbellReverseLunge = "fe36fe89-b99f-46d6-abcc-992bf2c5348c" // Dumbbell Reverse Lunge
        const val dumbbellRomanianDeadlift = "2c3dddd6-ed21-56e0-93cb-72fe9bdfb24c" // Dumbbell Romanian Deadlift
        const val dumbbellShrug = "c0d42a30-4dbf-40f0-b207-0232bdf9aa0d" // Dumbbell Shrug
        const val dumbbellStepUp = "0e87cefa-6794-576c-bd7d-ddfd400ff6ba" // Dumbbell Step-Up
        const val dumbbellSumoSquat = "6556e3cf-1dc5-4c5a-bd97-d8e52747a590" // Dumbbell Sumo Squat
        const val elbowPlank = "f4a0dafb-3fbd-44a4-9917-ff92e4667cef" // Elbow Plank
        const val gobletSquat = "7479a68d-667e-42b4-8c02-6f5c7743de1c" // Goblet Squat
        const val hangingKneeRaise = "1e7736da-08b9-4624-bb70-081c9e813dbc" // Hanging Knee Raise
        const val inclineDumbbellBenchPress = "b26385f3-9254-439e-b69f-f4eec8157843" // Incline Dumbbell Bench Press
        const val kneelingBodyweightTricepsExtension = "7d8d1cb4-7736-568e-814f-a976c0ddb26e" // Kneeling Bodyweight Triceps Extension
        const val kneelingPushUp = "b00ee097-984c-5303-b576-72560f73e7b6" // Kneeling Push-Up
        const val latPulldown = "da299546-de47-46b8-8829-261e85bf66ef" // Lat Pulldown
        const val legExtension = "7396b52d-76bb-5c97-8e92-92ebce73a500" // Leg Extension
        const val legPress = "dba621a2-9e6a-5c1b-8430-e5b509d05a26" // Leg Press
        const val machineChestPress = "cc0f47dd-bf6c-50e3-a5ce-be4658f292cc" // Machine Chest Press
        const val militaryPress = "a428a2aa-53d9-495b-8ac3-7bb2e9d9425f" // Military Press
        const val oneArmDumbbellRow = "2c132c78-1c40-412d-b3c3-c8a59fdfc4ee" // One-Arm Dumbbell Row
        const val pikePushUp = "6fdbfa91-ac2a-51ce-94c1-48b9fa2fffbe" // Pike Push-Up
        const val pushUp = "ae592bb3-22fa-44b1-a584-0020912cdad4" // Push-Up
        const val reverseCrunch = "94069880-626c-5d24-a1da-fd23dfde5657" // Reverse Crunch
        const val reverseLunge = "bd817796-b999-42d5-a4fc-359319ad0bdc" // Reverse Lunge
        const val romanianDeadlift = "7a060e5a-a8d0-46b0-84af-1ecd2671be9d" // Romanian Deadlift
        const val russianTwist = "78402cfa-ef6a-4fc2-82e6-8aa6d008a8e4" // Russian Twist
        const val seatedCableRow = "da0ae376-879e-5f05-8de6-cf0b3dc827a1" // Seated Cable Row
        const val seatedCalfRaise = "0a5781b5-8f9b-4937-9ef5-341f0f75bbda" // Seated Calf Raise
        const val seatedDumbbellOverheadExtension = "fedbe2c6-a1cb-4203-97e4-01a1f8aa6a83" // Seated Dumbbell Overhead Extension
        const val seatedDumbbellShoulderPress = "bc29d6c5-b146-47c5-b5bb-3e41d94d70a0" // Seated Dumbbell Shoulder Press
        const val seatedLegCurl = "2dd8bcc8-383a-5f2f-ba21-00261640abd3" // Seated Leg Curl
        const val sideLunge = "88c8135e-2a2c-5619-a3da-9b34bdc7b86b" // Side Lunge
        const val sidePlank = "249f99cc-a714-42af-b26f-70e710f74db8" // Side Plank
        const val singleLegBodyweightRomanianDeadlift = "eed8a374-5212-5564-bd53-ac3219fe7c73" // Single-Leg Bodyweight Romanian Deadlift
        const val singleLegDumbbellCalfRaise = "9e03139a-ee4d-4902-8dcf-d30c73f44a66" // Single-Leg Dumbbell Calf Raise
        const val skullCrusher = "fa7ad1c9-7474-4aaf-9f88-185d3f5e4918" // Skull Crusher
        const val supermanHold = "6b38fc07-e59f-4a6b-b94f-97880838e9e3" // Superman Hold
        const val supermanPull = "c12e37bb-c722-5fa8-b8ed-db242e3154b2" // Superman Pull
        const val wideGripPushUp = "74db5cdc-1762-5279-82b3-5e60843cb02e" // Wide-Grip Push-Up
    }
}
