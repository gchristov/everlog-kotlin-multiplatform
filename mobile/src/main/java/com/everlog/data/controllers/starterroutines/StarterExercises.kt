package com.everlog.data.controllers.starterroutines

import com.everlog.data.controllers.starterroutines.StarterProfile.Place

/**
 * The exercises in each starter routine, from the global exercise library
 * (`global/exercises/all`, the same in dev and prod). Each day lists 6, main lifts first, and
 * shorter routines take the first few. Full body C only goes to those just starting, so it lists 4.
 */
internal object StarterExercises {

    // A hold is timed rather than counted in reps. Its name and image come from the library;
    // the comments only say which exercise each uuid is today.
    data class Exercise(val uuid: String, val hold: Boolean = false)

    enum class Day { FULL_BODY_A, FULL_BODY_B, FULL_BODY_C, UPPER, LOWER, PUSH, PULL, LEGS }

    fun forDay(place: Place, day: Day): List<Exercise> = when (place) {
        Place.GYM -> gym(day)
        Place.HOME_DUMBBELLS -> homeDumbbells(day)
        Place.BODYWEIGHT -> bodyweight(day)
    }

    // Barbells, machines, cables
    private fun gym(day: Day) = when (day) {
        Day.FULL_BODY_A -> listOf(BarbellSquat, BarbellBenchPress, BarbellBentOverRow, RomanianDeadlift, DumbbellLateralRaise, ElbowPlank)
        Day.FULL_BODY_B -> listOf(BarbellDeadlift, MilitaryPress, LatPulldown, BulgarianSplitSquat, InclineDumbbellBenchPress, CablePushdown)
        Day.FULL_BODY_C -> listOf(Deadlift, InclineBarbellBenchPress, OneArmDumbbellRow, ForwardLunge)
        Day.UPPER -> listOf(BarbellBenchPress, BarbellBentOverRow, MilitaryPress, LatPulldown, DumbbellLateralRaise, CablePushdown)
        Day.LOWER -> listOf(BarbellSquat, RomanianDeadlift, BulgarianSplitSquat, BarbellHipThrust, SeatedCalfRaise, HangingKneeRaise)
        Day.PUSH -> listOf(BarbellBenchPress, MilitaryPress, InclineDumbbellBenchPress, DumbbellLateralRaise, CablePushdown, SkullCrusher)
        Day.PULL -> listOf(BarbellDeadlift, PullUp, BarbellBentOverRow, LatPulldown, CableFacePull, EZBarCurl)
        Day.LEGS -> listOf(BarbellSquat, StraightLegDeadlift, ForwardLunge, BarbellHipThrust, SingleLegDumbbellCalfRaise, HangingKneeRaise)
    }

    // Dumbbells and a bit of space, so no bench presses
    private fun homeDumbbells(day: Day) = when (day) {
        Day.FULL_BODY_A -> listOf(GobletSquat, DumbbellFloorPress, OneArmDumbbellRow, DumbbellReverseLunge, SeatedDumbbellShoulderPress, ElbowPlank)
        Day.FULL_BODY_B -> listOf(DumbbellSumoSquat, PushUp, DumbbellPullover, DumbbellHipThrust, DumbbellLateralRaise, RussianTwist)
        Day.FULL_BODY_C -> listOf(BulgarianSplitSquat, ArnoldPress, ChestSupportedDumbbellReverseFly, DumbbellSideSquat)
        Day.UPPER -> listOf(DumbbellFloorPress, OneArmDumbbellRow, SeatedDumbbellShoulderPress, ChestSupportedDumbbellReverseFly, DumbbellCurl, SeatedDumbbellOverheadExtension)
        Day.LOWER -> listOf(GobletSquat, DumbbellReverseLunge, BulgarianSplitSquat, DumbbellHipThrust, SingleLegDumbbellCalfRaise, ElbowPlank)
        Day.PUSH -> listOf(DumbbellFloorPress, SeatedDumbbellShoulderPress, PushUp, DumbbellLateralRaise, SeatedDumbbellOverheadExtension, DumbbellKickback)
        Day.PULL -> listOf(OneArmDumbbellRow, DumbbellPullover, ChestSupportedDumbbellReverseFly, DumbbellShrug, DumbbellHammerCurl, DumbbellCurl)
        Day.LEGS -> listOf(GobletSquat, BulgarianSplitSquat, DumbbellSumoSquat, DumbbellHipThrust, SingleLegDumbbellCalfRaise, RussianTwist)
    }

    // No equipment, not even a pull-up bar, so pulling is back holds and core
    private fun bodyweight(day: Day) = when (day) {
        Day.FULL_BODY_A -> listOf(BodyweightSquat, PushUp, ReverseLunge, SupermanHold, BodyweightCalfRaise, ElbowPlank)
        Day.FULL_BODY_B -> listOf(ForwardLunge, DeclinePushUp, DonkeyKick, BenchDip, Cobra, BicycleCrunch)
        Day.FULL_BODY_C -> listOf(BodyweightSquat, CloseGripPushUp, ReverseLunge, SupermanHold)
        Day.UPPER -> listOf(PushUp, SupermanHold, DeclinePushUp, BenchDip, Cobra, ElbowPlank)
        Day.LOWER -> listOf(BodyweightSquat, ReverseLunge, ForwardLunge, DonkeyKick, BodyweightCalfRaise, SidePlank)
        Day.PUSH -> listOf(PushUp, DeclinePushUp, CloseGripPushUp, TigerPushUp, BenchDip, ElbowPlank)
        Day.PULL -> listOf(SupermanHold, Cobra, PlankReachThrough, SidePlank, VCrunch, BicycleCrunch)
        Day.LEGS -> listOf(BodyweightSquat, ReverseLunge, ForwardLunge, DonkeyKick, BodyweightCalfRaise, PistolSquat)
    }

    // Abs
    private val BicycleCrunch = Exercise("2b1d7c68-15f5-44a3-9f25-0b69b7cb97d6") // Bicycle Crunch
    private val ElbowPlank = Exercise("f4a0dafb-3fbd-44a4-9917-ff92e4667cef", hold = true) // Elbow Plank
    private val HangingKneeRaise = Exercise("1e7736da-08b9-4624-bb70-081c9e813dbc") // Hanging Knee Raise
    private val PlankReachThrough = Exercise("4a74516d-21d4-4614-8d30-a4201fbd65b5") // Plank Reach Through
    private val RussianTwist = Exercise("78402cfa-ef6a-4fc2-82e6-8aa6d008a8e4") // Russian Twist
    private val SidePlank = Exercise("249f99cc-a714-42af-b26f-70e710f74db8", hold = true) // Side Plank
    private val VCrunch = Exercise("a4904689-5cc8-454b-b8de-e67e20f8490e") // V Crunch

    // Arms
    private val BenchDip = Exercise("f97f7709-729b-4863-b531-1aac552e6a7d") // Bench Dip
    private val CablePushdown = Exercise("c8ba67c1-298b-42aa-a481-66ae072ee857") // Cable Pushdown
    private val CloseGripPushUp = Exercise("a6c1a5e6-efae-4065-b8ab-78cb2e60263b") // Close-Grip Push-Up
    private val DumbbellCurl = Exercise("e922c80e-1a09-444e-b49c-20255eb0cfb1") // Dumbbell Curl
    private val DumbbellHammerCurl = Exercise("02ad17ce-0398-4038-bd2a-2f4b19446083") // Dumbbell Hammer Curl
    private val DumbbellKickback = Exercise("ffa01a27-3f09-4ba2-b9bc-fa8404800c8e") // Dumbbell Kickback
    private val EZBarCurl = Exercise("44a03415-12b1-4ec3-a567-4bdbdc99256c") // EZ Bar Curl
    private val SeatedDumbbellOverheadExtension = Exercise("fedbe2c6-a1cb-4203-97e4-01a1f8aa6a83") // Seated Dumbbell Overhead Extension
    private val SkullCrusher = Exercise("fa7ad1c9-7474-4aaf-9f88-185d3f5e4918") // Skull Crusher
    private val TigerPushUp = Exercise("92e7eafd-401b-467a-b3b0-d202c9041b53") // Tiger Push Up

    // Back
    private val BarbellBentOverRow = Exercise("16112844-fe70-48d3-9738-9dd2b9245fa9") // Barbell Bent-Over Row
    private val Cobra = Exercise("7a9a92a9-2d7d-418f-addd-722af5dd7e79", hold = true) // Cobra
    private val DumbbellPullover = Exercise("dd0931e6-2370-4b1b-a53d-36e28814f573") // Dumbbell Pullover
    private val LatPulldown = Exercise("da299546-de47-46b8-8829-261e85bf66ef") // Lat Pulldown
    private val OneArmDumbbellRow = Exercise("2c132c78-1c40-412d-b3c3-c8a59fdfc4ee") // One-Arm Dumbbell Row
    private val PullUp = Exercise("dda9a745-6ab4-4db7-948d-ce32fe0f4213") // Pull Up
    private val SupermanHold = Exercise("6b38fc07-e59f-4a6b-b94f-97880838e9e3", hold = true) // Superman Hold

    // Chest
    private val BarbellBenchPress = Exercise("a30b9d26-9079-41ed-a3d4-9f1c309f200e") // Barbell Bench Press
    private val DeclinePushUp = Exercise("11de1ab8-e2de-4213-af98-991316042fcb") // Decline Push-Up
    private val DumbbellFloorPress = Exercise("afdba2b4-941b-4bf4-99db-06da1370744d") // Dumbbell Floor Press
    private val InclineBarbellBenchPress = Exercise("773c52e9-d802-4e67-8945-afc363d53a4e") // Incline Barbell Bench Press
    private val InclineDumbbellBenchPress = Exercise("b26385f3-9254-439e-b69f-f4eec8157843") // Incline Dumbbell Bench Press
    private val PushUp = Exercise("ae592bb3-22fa-44b1-a584-0020912cdad4") // Push-Up

    // Legs
    private val BarbellDeadlift = Exercise("fccec58d-97db-4d3c-9584-41a71ddbe72c") // Barbell Deadlift
    private val BarbellHipThrust = Exercise("3689ed44-836a-404c-831a-7ec1f1ac2374") // Barbell Hip Thrust
    private val BarbellSquat = Exercise("8d2d3fcb-7084-4932-ad62-25c2be2823f9") // Barbell Squat
    private val BodyweightCalfRaise = Exercise("8c81478e-e98b-5f0e-8ae3-ccd77dd44bcd") // Bodyweight Calf Raise
    private val BodyweightSquat = Exercise("4d92c102-e94a-5f71-bfde-b49938a71e80") // Bodyweight Squat
    private val BulgarianSplitSquat = Exercise("c1cd66bd-db95-46b4-93cb-70944bcbee97") // Bulgarian Split Squat
    private val Deadlift = Exercise("7b41837e-9300-4cfa-80aa-e1a47b4685ff") // Deadlift
    private val DonkeyKick = Exercise("7d992f3c-014b-4cb2-ad0e-8046e16431a0") // Donkey Kick
    private val DumbbellHipThrust = Exercise("a21cfe26-5ac9-57a0-8d63-f9cf8df2a69f") // Dumbbell Hip Thrust
    private val DumbbellReverseLunge = Exercise("fe36fe89-b99f-46d6-abcc-992bf2c5348c") // Dumbbell Reverse Lunge
    private val DumbbellSideSquat = Exercise("e5bb0eb8-6758-4b88-b0eb-cd79e56a5677") // Dumbbell Side Squat
    private val DumbbellSumoSquat = Exercise("6556e3cf-1dc5-4c5a-bd97-d8e52747a590") // Dumbbell Sumo Squat
    private val ForwardLunge = Exercise("068a5a00-15dc-450e-a0d2-2b6e1c65c983") // Forward Lunge
    private val GobletSquat = Exercise("7479a68d-667e-42b4-8c02-6f5c7743de1c") // Goblet Squat
    private val PistolSquat = Exercise("08d62221-8ba6-452d-a534-f4187dcf63f1") // Pistol Squat
    private val ReverseLunge = Exercise("bd817796-b999-42d5-a4fc-359319ad0bdc") // Reverse Lunge
    private val RomanianDeadlift = Exercise("7a060e5a-a8d0-46b0-84af-1ecd2671be9d") // Romanian Deadlift
    private val SeatedCalfRaise = Exercise("0a5781b5-8f9b-4937-9ef5-341f0f75bbda") // Seated Calf Raise
    private val SingleLegDumbbellCalfRaise = Exercise("9e03139a-ee4d-4902-8dcf-d30c73f44a66") // Single-Leg Dumbbell Calf Raise
    private val StraightLegDeadlift = Exercise("4c8d109b-c959-4214-a0de-cdadd9caeb75") // Straight Leg Deadlift

    // Shoulders
    private val ArnoldPress = Exercise("ed1bfc44-bd75-4d12-8e41-2740b010c54c") // Arnold Press
    private val CableFacePull = Exercise("2851b948-ebd6-4ca2-8bd8-84f5a0b8b637") // Cable Face Pull
    private val ChestSupportedDumbbellReverseFly = Exercise("93729477-7e69-4f22-8e0b-27f204aeab58") // Chest-Supported Dumbbell Reverse Fly
    private val DumbbellLateralRaise = Exercise("a273b91b-fad3-4609-975c-d5f2077ebc0e") // Dumbbell Lateral Raise
    private val DumbbellShrug = Exercise("c0d42a30-4dbf-40f0-b207-0232bdf9aa0d") // Dumbbell Shrug
    private val MilitaryPress = Exercise("a428a2aa-53d9-495b-8ac3-7bb2e9d9425f") // Military Press
    private val SeatedDumbbellShoulderPress = Exercise("bc29d6c5-b146-47c5-b5bb-3e41d94d70a0") // Seated Dumbbell Shoulder Press
}
