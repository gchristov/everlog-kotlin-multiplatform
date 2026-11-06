package com.everlog.android.history.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.kotlin.buildWeekDays
import com.everlog.android.common.kotlin.fromJava
import com.everlog.android.common.kotlin.toJava
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import java.util.Locale
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.TextStyle

/**
 * The months from the oldest workout shown to this month, opening on it. Reaching the oldest month asks for
 * older workouts, which add months above without moving the ones in view.
 */
@Composable
internal fun HistoryCalendar(
    state: HistoryPageState,
    contentPadding: PaddingValues,
    onDayPicked: (LocalDate) -> Unit,
    onReachedOldest: () -> Unit,
) {
    val months = calendarMonths(state.days, state.today)
    val workoutDays = remember(state.days) { state.days.mapTo(HashSet()) { it.date } }
    // Built from the first values only: the library makes a new state, back on this month, whenever its arguments
    // change, so later months and settings go through its setters below
    val initial = remember { InitialCalendar(months, YearMonth.from(state.today), state.firstDayOfWeek) }
    val calendar = rememberCalendarState(
        startMonth = initial.months.start.toJavaMonth(),
        endMonth = initial.months.endInclusive.toJavaMonth(),
        firstVisibleMonth = initial.opensOn.toJavaMonth(),
        firstDayOfWeek = initial.firstDayOfWeek.toJava(),
    )
    LaunchedEffect(months, state.firstDayOfWeek) {
        calendar.startMonth = months.start.toJavaMonth()
        calendar.endMonth = months.endInclusive.toJavaMonth()
        calendar.firstDayOfWeek = state.firstDayOfWeek.toJava()
    }
    val atOldest by remember { derivedStateOf { calendar.firstVisibleMonth.yearMonth <= calendar.startMonth } }
    // Asked again as each page lands, until one adds a month or the first workout shows
    LaunchedEffect(atOldest, state.days.size, state.loadingMore) {
        if (atOldest) onReachedOldest()
    }
    VerticalCalendar(
        modifier = Modifier.fillMaxSize().testTag(HistoryPageTags.Calendar),
        state = calendar,
        contentPadding = contentPadding,
        monthHeader = { month -> MonthHeader(month, state) },
        dayContent = { day ->
            val date = day.date.fromJava()
            CalendarDayCell(
                date = date,
                inMonth = day.position == DayPosition.MonthDate,
                hasWorkouts = date in workoutDays,
                state = state,
                onPicked = onDayPicked,
            )
        },
    )
}

@Composable
private fun MonthHeader(month: CalendarMonth, state: HistoryPageState) {
    Column(modifier = Modifier.padding(bottom = Theme.spacing.small)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = CalendarSizes.MonthHeaderHeight).padding(horizontal = Theme.spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large),
        ) {
            val yearMonth = month.yearMonth
            val name = yearMonth.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, Locale.getDefault())
            AppText(text = "$name ${yearMonth.year}", style = Theme.typography.subtitle)
            Box(modifier = Modifier.weight(1f).height(CalendarSizes.Line).background(Theme.backgrounds.separator))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            state.firstDayOfWeek.buildWeekDays().forEach { day ->
                AppText(
                    text = day.day?.first()?.uppercase().orEmpty(),
                    style = Theme.typography.small,
                    color = Theme.contentColors.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// A square with the day's number: a dot when it has workouts, a ring for today, filled once picked. The ring and
// the fill take the whole cell, just past the square, as the calendar clips each day to its cell.
@Composable
private fun CalendarDayCell(
    date: LocalDate,
    inMonth: Boolean,
    hasWorkouts: Boolean,
    state: HistoryPageState,
    onPicked: (LocalDate) -> Unit,
) {
    if (!inMonth) return
    val future = date.isAfter(state.today)
    val selected = date == state.selected
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .alpha(if (future && !selected) CalendarSizes.FutureAlpha else 1f)
            .clickable(enabled = !future) { onPicked(date) }
            .testTag(HistoryPageTags.day(date)),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(CalendarSizes.DayGap).border(CalendarSizes.Line, Theme.backgrounds.separator))
        if (date == state.today && !selected) {
            Box(modifier = Modifier.fillMaxSize().border(CalendarSizes.TodayRing, Theme.contentColors.action, CircleShape))
        }
        if (hasWorkouts) {
            Box(
                modifier = Modifier.padding(CalendarSizes.DayGap + CalendarSizes.DotMargin).size(CalendarSizes.Dot)
                    .background(Theme.contentColors.action)
                    .testTag(HistoryPageTags.WorkoutDot),
            )
        }
        if (selected) {
            Box(modifier = Modifier.fillMaxSize().background(Theme.contentColors.action, CircleShape))
            AppText(
                text = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                style = Theme.typography.bodyBold,
                color = Theme.contentColors.onAction,
                modifier = Modifier.align(Alignment.TopStart).padding(start = CalendarSizes.SelectedTextMargin, top = CalendarSizes.SelectedTextMargin),
            )
        }
        AppText(
            text = date.dayOfMonth.toString(),
            style = if (selected) Theme.typography.bodyBold else Theme.typography.body,
            color = if (selected) Theme.contentColors.onAction else Theme.contentColors.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(
                end = if (selected) CalendarSizes.SelectedTextMargin else CalendarSizes.DayGap + CalendarSizes.TextMargin,
                bottom = if (selected) CalendarSizes.SelectedTextMargin else CalendarSizes.DayGap,
            ),
        )
    }
}

private class InitialCalendar(val months: ClosedRange<YearMonth>, val opensOn: YearMonth, val firstDayOfWeek: DayOfWeek)

private fun YearMonth.toJavaMonth(): java.time.YearMonth = java.time.YearMonth.of(year, monthValue)

private object CalendarSizes {
    val MonthHeaderHeight = 80.dp
    val DayGap = 3.dp
    val Line = 1.dp
    val TodayRing = 3.dp
    val Dot = 10.dp
    val DotMargin = 6.dp
    val TextMargin = 4.dp
    // Keeps the picked day's text inside its circle
    val SelectedTextMargin = 10.dp
    const val FutureAlpha = 0.5f
}
