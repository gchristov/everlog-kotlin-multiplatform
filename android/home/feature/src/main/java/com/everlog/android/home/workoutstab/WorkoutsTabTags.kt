package com.everlog.android.home.workoutstab

object WorkoutsTabTags {
    const val Exercises = "workouts:exercises"
    const val Card = "workouts:card"
    const val ActivePlan = "workouts:activePlan"
    const val Retry = "workouts:retry"

    fun template(uuid: String) = "workouts:template:$uuid"

    fun templateStart(uuid: String) = "workouts:template:$uuid:start"

    fun templateDetails(uuid: String) = "workouts:template:$uuid:details"

    fun plan(uuid: String) = "workouts:plan:$uuid"
}
