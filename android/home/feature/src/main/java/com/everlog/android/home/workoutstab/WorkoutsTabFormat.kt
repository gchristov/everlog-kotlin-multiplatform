package com.everlog.android.home.workoutstab

import com.everlog.android.common.data.model.set.ELSet
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit

// What an exercise's sets ask for, e.g. 3 × 8–12, 3 × 30 s, or just 3 sets when they don't say
sealed interface SetsTarget {
    val sets: Int

    data class Reps(override val sets: Int, val min: Int, val max: Int) : SetsTarget

    data class Time(override val sets: Int, val minSeconds: Int, val maxSeconds: Int) : SetsTarget

    data class Sets(override val sets: Int) : SetsTarget
}

// A template's sets keep a target or the values last done; either says what the set asks for
fun setsTarget(sets: List<ELSet>): SetsTarget {
    val reps = sets.mapNotNull { set -> set.getReps().takeIf { it > 0 } ?: set.getRequiredReps().takeIf { it > 0 } }
    if (sets.isNotEmpty() && reps.size == sets.size) {
        return SetsTarget.Reps(sets.size, reps.min(), reps.max())
    }
    val times = sets.mapNotNull { set ->
        (if (set.isTimeEntered()) set.getTimeSeconds() else null)?.takeIf { it > 0 }
            ?: (if (set.isRequiredTimeEntered()) set.getRequiredTimeSeconds() else null)?.takeIf { it > 0 }
    }
    if (sets.isNotEmpty() && times.size == sets.size) {
        return SetsTarget.Time(sets.size, times.min(), times.max())
    }
    return SetsTarget.Sets(sets.size)
}

// How long ago a template was last done, in calendar days
sealed interface DoneAgo {
    data object Today : DoneAgo

    data object Yesterday : DoneAgo

    data class Days(val days: Int) : DoneAgo

    data class Weeks(val weeks: Int) : DoneAgo

    // Not in the history the tab reads
    data object NotRecently : DoneAgo
}

fun doneAgo(lastDone: Long?, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DoneAgo {
    lastDone ?: return DoneAgo.NotRecently
    val days = ChronoUnit.DAYS.between(Instant.ofEpochMilli(lastDone).atZone(zone).toLocalDate(), today).toInt()
    return when {
        days <= 0 -> DoneAgo.Today
        days == 1 -> DoneAgo.Yesterday
        days < DaysPerWeek * 2 -> DoneAgo.Days(days)
        else -> DoneAgo.Weeks(days / DaysPerWeek)
    }
}

private const val DaysPerWeek = 7
