package com.everlog.android.home.workoutstab

// The library exercises whose images the tab's examples show, by uuid (the library's ids are the same in dev and prod)
internal object WorkoutsTabExamples {
    private const val BarbellBenchPress = "a30b9d26-9079-41ed-a3d4-9f1c309f200e"
    private const val MilitaryPress = "a428a2aa-53d9-495b-8ac3-7bb2e9d9425f"
    private const val InclineDumbbellBenchPress = "b26385f3-9254-439e-b69f-f4eec8157843"
    private const val BarbellDeadlift = "fccec58d-97db-4d3c-9584-41a71ddbe72c"
    private const val LatPulldown = "da299546-de47-46b8-8829-261e85bf66ef"
    private const val SeatedCableRow = "da0ae376-879e-5f05-8de6-cf0b3dc827a1"
    private const val BarbellBentOverRow = "16112844-fe70-48d3-9738-9dd2b9245fa9"
    private const val BarbellSquat = "8d2d3fcb-7084-4932-ad62-25c2be2823f9"

    val PushDay = listOf(BarbellBenchPress, MilitaryPress, InclineDumbbellBenchPress)
    val PullDay = listOf(BarbellDeadlift, LatPulldown, SeatedCableRow)
    const val UpperLowerCover = BarbellBentOverRow
    const val FullBodyCover = BarbellSquat

    val all = (PushDay + PullDay + UpperLowerCover + FullBodyCover).toSet()
}
