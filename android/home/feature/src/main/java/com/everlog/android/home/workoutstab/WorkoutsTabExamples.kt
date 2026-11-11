package com.everlog.android.home.workoutstab

// The images the tab's examples show: library exercises by uuid (the same in dev and prod), and Unsplash photos as plan covers
internal object WorkoutsTabExamples {
    private const val BarbellBenchPress = "a30b9d26-9079-41ed-a3d4-9f1c309f200e"
    private const val MilitaryPress = "a428a2aa-53d9-495b-8ac3-7bb2e9d9425f"
    private const val InclineDumbbellBenchPress = "b26385f3-9254-439e-b69f-f4eec8157843"
    private const val BarbellDeadlift = "fccec58d-97db-4d3c-9584-41a71ddbe72c"
    private const val LatPulldown = "da299546-de47-46b8-8829-261e85bf66ef"
    private const val SeatedCableRow = "da0ae376-879e-5f05-8de6-cf0b3dc827a1"

    val PushDay = listOf(BarbellBenchPress, MilitaryPress, InclineDumbbellBenchPress)
    val PullDay = listOf(BarbellDeadlift, LatPulldown, SeatedCableRow)
    val all = (PushDay + PullDay).toSet()

    // Unsplash photos, from the cover search plans use, at its thumbnail size: "person about to lift the barbell" by
    // Victor Freitas and "gym equipment inside room" by Risen Wang
    const val UpperLowerCover =
        "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w4OTE1OHwwfDF8YWxsfHx8fHx8fHx8MTc5NDQwNzIyNHw&ixlib=rb-4.1.0&q=80&w=200"
    const val FullBodyCover =
        "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w4OTE1OHwwfDF8YWxsfHx8fHx8fHx8MTc5NDQwNzIyNHw&ixlib=rb-4.1.0&q=80&w=200"
}
