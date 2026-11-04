package com.everlog.android.home

// What Home's tabs can ask of the Home screen around them, which implements it
interface HomeHost {
    fun showStatistics()

    fun showSettings()

    fun showPlans()

    // The start workout menu
    fun showAddMenu()

    // Home hides its start workout button on an empty week, which shows a button of its own
    fun setWeekEmpty(empty: Boolean)
}
