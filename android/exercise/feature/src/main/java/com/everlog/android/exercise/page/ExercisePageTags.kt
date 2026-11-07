package com.everlog.android.exercise.page

// Test tags for the page's parts, where text repeats or there's none
object ExercisePageTags {
    const val Pager = "exercisePagePager"
    const val Menu = "exercisePageMenu"
    const val Range = "exercisePageRange"
    const val Tiles = "exercisePageTiles"
    const val Charts = "exercisePageCharts"
    const val StatisticsLoading = "exercisePageStatisticsLoading"
    const val StatisticsExample = "exercisePageStatisticsExample"
    const val Feedback = "exercisePageFeedback"
    const val History = "exercisePageHistory"
    const val HistoryLoading = "exercisePageHistoryLoading"
    const val HistoryExample = "exercisePageHistoryExample"
    const val LoadingMore = "exercisePageLoadingMore"
    const val Snackbar = "exercisePageSnackbar"

    fun tab(tab: ExercisePageTab) = "exercisePageTab-${tab.name}"

    // By the session's key: its workout, and which of the exercise's sessions in it
    fun session(key: String) = "exercisePageSession-$key"
}
