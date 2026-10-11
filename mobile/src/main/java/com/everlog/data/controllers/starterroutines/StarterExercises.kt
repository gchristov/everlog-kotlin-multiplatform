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

    // Barbells, machines, cables. Those just starting get machines on Full body C
    private fun gym(day: Day) = when (day) {
        Day.FULL_BODY_A -> listOf(BarbellSquat, BarbellBenchPress, BarbellBentOverRow, RomanianDeadlift, DumbbellLateralRaise, ElbowPlank)
        Day.FULL_BODY_B -> listOf(BarbellDeadlift, MilitaryPress, LatPulldown, LegPress, InclineDumbbellBenchPress, CablePushdown)
        Day.FULL_BODY_C -> listOf(LegPress, MachineChestPress, SeatedCableRow, SeatedLegCurl)
        Day.UPPER -> listOf(BarbellBenchPress, BarbellBentOverRow, MilitaryPress, LatPulldown, BarbellCurl, CablePushdown)
        Day.LOWER -> listOf(BarbellSquat, RomanianDeadlift, LegPress, SeatedLegCurl, BarbellHipThrust, HangingKneeRaise)
        Day.PUSH -> listOf(BarbellBenchPress, MilitaryPress, InclineDumbbellBenchPress, DumbbellLateralRaise, CablePushdown, SkullCrusher)
        Day.PULL -> listOf(BarbellDeadlift, LatPulldown, SeatedCableRow, CableFacePull, BarbellCurl, DumbbellHammerCurl)
        Day.LEGS -> listOf(BarbellSquat, RomanianDeadlift, LegPress, LegExtension, SeatedLegCurl, SeatedCalfRaise)
    }

    // Dumbbells and a chair or step, but no bench
    private fun homeDumbbells(day: Day) = when (day) {
        Day.FULL_BODY_A -> listOf(GobletSquat, DumbbellFloorPress, OneArmDumbbellRow, DumbbellRomanianDeadlift, SeatedDumbbellShoulderPress, ElbowPlank)
        Day.FULL_BODY_B -> listOf(DumbbellReverseLunge, PushUp, DumbbellBentOverRow, DumbbellGluteBridge, DumbbellLateralRaise, DeadBug)
        Day.FULL_BODY_C -> listOf(DumbbellSumoSquat, ArnoldPress, OneArmDumbbellRow, DumbbellRomanianDeadlift)
        Day.UPPER -> listOf(DumbbellFloorPress, OneArmDumbbellRow, SeatedDumbbellShoulderPress, DumbbellBentOverRow, DumbbellCurl, SeatedDumbbellOverheadExtension)
        Day.LOWER -> listOf(GobletSquat, DumbbellRomanianDeadlift, DumbbellBulgarianSplitSquat, DumbbellGluteBridge, SingleLegDumbbellCalfRaise, ElbowPlank)
        Day.PUSH -> listOf(DumbbellFloorPress, SeatedDumbbellShoulderPress, PushUp, DumbbellLateralRaise, SeatedDumbbellOverheadExtension, DumbbellKickback)
        Day.PULL -> listOf(OneArmDumbbellRow, DumbbellBentOverRow, BentOverDumbbellRearDeltFly, DumbbellCurl, DumbbellHammerCurl, DumbbellShrug)
        Day.LEGS -> listOf(GobletSquat, DumbbellRomanianDeadlift, DumbbellReverseLunge, DumbbellStepUp, SingleLegDumbbellCalfRaise, RussianTwist)
    }

    // Floor only: no bar, chair or table, so pulling is lying face down (Superman Pull and Hold)
    private fun bodyweight(day: Day) = when (day) {
        Day.FULL_BODY_A -> listOf(BodyweightSquat, PushUp, SupermanPull, BodyweightGluteBridge, ReverseLunge, ElbowPlank)
        Day.FULL_BODY_B -> listOf(ReverseLunge, PikePushUp, SupermanHold, SingleLegBodyweightRomanianDeadlift, CloseGripPushUp, DeadBug)
        Day.FULL_BODY_C -> listOf(BodyweightSquat, KneelingPushUp, SupermanPull, BodyweightGluteBridge)
        Day.UPPER -> listOf(PushUp, SupermanPull, PikePushUp, SupermanHold, KneelingBodyweightTricepsExtension, ElbowPlank)
        Day.LOWER -> listOf(BodyweightSquat, BodyweightGluteBridge, ReverseLunge, SingleLegBodyweightRomanianDeadlift, BodyweightCalfRaise, SidePlank)
        Day.PUSH -> listOf(PushUp, PikePushUp, WideGripPushUp, CloseGripPushUp, KneelingBodyweightTricepsExtension, ElbowPlank)
        Day.PULL -> listOf(SupermanPull, SupermanHold, BirdDog, ReverseCrunch, SidePlank, BicycleCrunch)
        Day.LEGS -> listOf(BodyweightSquat, ReverseLunge, BodyweightGluteBridge, SideLunge, SingleLegBodyweightRomanianDeadlift, BodyweightCalfRaise)
    }

    // Abs
    private val BicycleCrunch = Exercise("2b1d7c68-15f5-44a3-9f25-0b69b7cb97d6") // Bicycle Crunch
    private val BirdDog = Exercise("d652a86a-98f8-5bf2-a1f4-b1b601491599") // Bird Dog
    private val DeadBug = Exercise("b6d7db4a-e3cc-58d4-9387-df0f25d4ef44") // Dead Bug
    private val ElbowPlank = Exercise("f4a0dafb-3fbd-44a4-9917-ff92e4667cef", hold = true) // Elbow Plank
    private val HangingKneeRaise = Exercise("1e7736da-08b9-4624-bb70-081c9e813dbc") // Hanging Knee Raise
    private val ReverseCrunch = Exercise("94069880-626c-5d24-a1da-fd23dfde5657") // Reverse Crunch
    private val RussianTwist = Exercise("78402cfa-ef6a-4fc2-82e6-8aa6d008a8e4") // Russian Twist
    private val SidePlank = Exercise("249f99cc-a714-42af-b26f-70e710f74db8", hold = true) // Side Plank

    // Arms
    private val BarbellCurl = Exercise("6bb3fc82-b228-5354-aeef-50dc481ab462") // Barbell Curl
    private val CablePushdown = Exercise("c8ba67c1-298b-42aa-a481-66ae072ee857") // Cable Pushdown
    private val CloseGripPushUp = Exercise("a6c1a5e6-efae-4065-b8ab-78cb2e60263b") // Close-Grip Push-Up
    private val DumbbellCurl = Exercise("e922c80e-1a09-444e-b49c-20255eb0cfb1") // Dumbbell Curl
    private val DumbbellHammerCurl = Exercise("02ad17ce-0398-4038-bd2a-2f4b19446083") // Dumbbell Hammer Curl
    private val DumbbellKickback = Exercise("ffa01a27-3f09-4ba2-b9bc-fa8404800c8e") // Dumbbell Kickback
    private val KneelingBodyweightTricepsExtension = Exercise("7d8d1cb4-7736-568e-814f-a976c0ddb26e") // Kneeling Bodyweight Triceps Extension
    private val SeatedDumbbellOverheadExtension = Exercise("fedbe2c6-a1cb-4203-97e4-01a1f8aa6a83") // Seated Dumbbell Overhead Extension
    private val SkullCrusher = Exercise("fa7ad1c9-7474-4aaf-9f88-185d3f5e4918") // Skull Crusher

    // Back
    private val BarbellBentOverRow = Exercise("16112844-fe70-48d3-9738-9dd2b9245fa9") // Barbell Bent-Over Row
    private val DumbbellBentOverRow = Exercise("e4d38a62-3615-59ba-81f5-3ed42b688d7a") // Dumbbell Bent-Over Row
    private val LatPulldown = Exercise("da299546-de47-46b8-8829-261e85bf66ef") // Lat Pulldown
    private val OneArmDumbbellRow = Exercise("2c132c78-1c40-412d-b3c3-c8a59fdfc4ee") // One-Arm Dumbbell Row
    private val SeatedCableRow = Exercise("da0ae376-879e-5f05-8de6-cf0b3dc827a1") // Seated Cable Row
    private val SupermanHold = Exercise("6b38fc07-e59f-4a6b-b94f-97880838e9e3", hold = true) // Superman Hold
    private val SupermanPull = Exercise("c12e37bb-c722-5fa8-b8ed-db242e3154b2") // Superman Pull

    // Chest
    private val BarbellBenchPress = Exercise("a30b9d26-9079-41ed-a3d4-9f1c309f200e") // Barbell Bench Press
    private val DumbbellFloorPress = Exercise("afdba2b4-941b-4bf4-99db-06da1370744d") // Dumbbell Floor Press
    private val InclineDumbbellBenchPress = Exercise("b26385f3-9254-439e-b69f-f4eec8157843") // Incline Dumbbell Bench Press
    private val KneelingPushUp = Exercise("b00ee097-984c-5303-b576-72560f73e7b6") // Kneeling Push-Up
    private val MachineChestPress = Exercise("cc0f47dd-bf6c-50e3-a5ce-be4658f292cc") // Machine Chest Press
    private val PushUp = Exercise("ae592bb3-22fa-44b1-a584-0020912cdad4") // Push-Up
    private val WideGripPushUp = Exercise("74db5cdc-1762-5279-82b3-5e60843cb02e") // Wide-Grip Push-Up

    // Legs
    private val BarbellDeadlift = Exercise("fccec58d-97db-4d3c-9584-41a71ddbe72c") // Barbell Deadlift
    private val BarbellHipThrust = Exercise("3689ed44-836a-404c-831a-7ec1f1ac2374") // Barbell Hip Thrust
    private val BarbellSquat = Exercise("8d2d3fcb-7084-4932-ad62-25c2be2823f9") // Barbell Squat
    private val BodyweightCalfRaise = Exercise("8c81478e-e98b-5f0e-8ae3-ccd77dd44bcd") // Bodyweight Calf Raise
    private val BodyweightGluteBridge = Exercise("5338dea5-7d97-5f32-8f49-6dc0bcdc6b84") // Bodyweight Glute Bridge
    private val BodyweightSquat = Exercise("4d92c102-e94a-5f71-bfde-b49938a71e80") // Bodyweight Squat
    private val DumbbellBulgarianSplitSquat = Exercise("b5410e06-9c17-5f1b-9eea-ef6b3a1adfc8") // Dumbbell Bulgarian Split Squat
    private val DumbbellGluteBridge = Exercise("89c5c37c-2098-509e-bc14-080f3e7d5e2e") // Dumbbell Glute Bridge
    private val DumbbellReverseLunge = Exercise("fe36fe89-b99f-46d6-abcc-992bf2c5348c") // Dumbbell Reverse Lunge
    private val DumbbellRomanianDeadlift = Exercise("2c3dddd6-ed21-56e0-93cb-72fe9bdfb24c") // Dumbbell Romanian Deadlift
    private val DumbbellStepUp = Exercise("0e87cefa-6794-576c-bd7d-ddfd400ff6ba") // Dumbbell Step-Up
    private val DumbbellSumoSquat = Exercise("6556e3cf-1dc5-4c5a-bd97-d8e52747a590") // Dumbbell Sumo Squat
    private val GobletSquat = Exercise("7479a68d-667e-42b4-8c02-6f5c7743de1c") // Goblet Squat
    private val LegExtension = Exercise("7396b52d-76bb-5c97-8e92-92ebce73a500") // Leg Extension
    private val LegPress = Exercise("dba621a2-9e6a-5c1b-8430-e5b509d05a26") // Leg Press
    private val ReverseLunge = Exercise("bd817796-b999-42d5-a4fc-359319ad0bdc") // Reverse Lunge
    private val RomanianDeadlift = Exercise("7a060e5a-a8d0-46b0-84af-1ecd2671be9d") // Romanian Deadlift
    private val SeatedCalfRaise = Exercise("0a5781b5-8f9b-4937-9ef5-341f0f75bbda") // Seated Calf Raise
    private val SeatedLegCurl = Exercise("2dd8bcc8-383a-5f2f-ba21-00261640abd3") // Seated Leg Curl
    private val SideLunge = Exercise("88c8135e-2a2c-5619-a3da-9b34bdc7b86b") // Side Lunge
    private val SingleLegBodyweightRomanianDeadlift = Exercise("eed8a374-5212-5564-bd53-ac3219fe7c73") // Single-Leg Bodyweight Romanian Deadlift
    private val SingleLegDumbbellCalfRaise = Exercise("9e03139a-ee4d-4902-8dcf-d30c73f44a66") // Single-Leg Dumbbell Calf Raise

    // Shoulders
    private val ArnoldPress = Exercise("ed1bfc44-bd75-4d12-8e41-2740b010c54c") // Arnold Press
    private val BentOverDumbbellRearDeltFly = Exercise("5ebd43ce-c87f-4833-9fbf-6c14ccd8dba8") // Bent-Over Dumbbell Rear Delt Fly
    private val CableFacePull = Exercise("2851b948-ebd6-4ca2-8bd8-84f5a0b8b637") // Cable Face Pull
    private val DumbbellLateralRaise = Exercise("a273b91b-fad3-4609-975c-d5f2077ebc0e") // Dumbbell Lateral Raise
    private val DumbbellShrug = Exercise("c0d42a30-4dbf-40f0-b207-0232bdf9aa0d") // Dumbbell Shrug
    private val MilitaryPress = Exercise("a428a2aa-53d9-495b-8ac3-7bb2e9d9425f") // Military Press
    private val PikePushUp = Exercise("6fdbfa91-ac2a-51ce-94c1-48b9fa2fffbe") // Pike Push-Up
    private val SeatedDumbbellShoulderPress = Exercise("bc29d6c5-b146-47c5-b5bb-3e41d94d70a0") // Seated Dumbbell Shoulder Press
}
