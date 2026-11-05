package com.everlog.android.history.ui.page

import org.threeten.bp.LocalDate

// For tests to find the history page's parts
object HistoryPageTags {
    const val View = "history.view"
    const val Loading = "history.loading"
    const val LoadingMore = "history.loadingMore"
    const val Empty = "history.empty"
    const val EmptyDay = "history.emptyDay"
    const val Calendar = "history.calendar"
    const val List = "history.list"
    const val WorkoutDot = "history.workoutDot"

    // A day on the calendar or in the list, whichever shows
    fun day(date: LocalDate) = "history.day.$date"
}
