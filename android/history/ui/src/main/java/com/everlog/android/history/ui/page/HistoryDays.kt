package com.everlog.android.history.ui.page

import com.everlog.android.common.data.model.workout.ELWorkout
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.ZoneId

// One day of the history: its workouts, earliest first
data class HistoryDay(val date: LocalDate, val workouts: List<ELWorkout>)

// The workouts by the day they finished, oldest day first, so the newest sits at the bottom like a chat
fun historyDays(workouts: List<ELWorkout>, zone: ZoneId = ZoneId.systemDefault()): List<HistoryDay> =
    workouts
        .sortedBy { it.completedDate }
        .groupBy { Instant.ofEpochMilli(it.completedDate).atZone(zone).toLocalDate() }
        .map { (date, onDay) -> HistoryDay(date, onDay) }

/**
 * The newest days, up to [workouts] workouts in all, as the first page of a read holds: what's kept while the page
 * is away. The newest day is kept whole, however many it has.
 */
fun List<HistoryDay>.newestPage(workouts: Int): List<HistoryDay> {
    var kept = 0
    val newest = takeLastWhile { day ->
        val keep = kept == 0 || kept + day.workouts.size <= workouts
        if (keep) kept += day.workouts.size
        keep
    }
    return newest
}

/**
 * The calendar's months: from the oldest day shown so far, which moves back as older pages load, to [today]'s.
 * Just this month while there's nothing to show.
 */
fun calendarMonths(days: List<HistoryDay>, today: LocalDate): ClosedRange<YearMonth> {
    val now = YearMonth.from(today)
    val first = days.firstOrNull()?.let { YearMonth.from(it.date) }?.takeIf { it < now } ?: now
    return first..now
}
