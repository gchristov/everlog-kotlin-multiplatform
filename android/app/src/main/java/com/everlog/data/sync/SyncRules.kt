package com.everlog.data.sync

import java.util.Calendar
import java.util.concurrent.TimeUnit

// What a sync does
internal enum class SyncWork {
    // The whole collection or document, in one read
    Whole,

    // The last months of a SyncPlan.Recent collection, then a count check once its full history has synced
    Recent,

    // A SyncPlan.Recent collection's full history, in chunks
    FullHistory;

    // A sync running [this] makes asking for [other] unnecessary
    fun covers(other: SyncWork): Boolean = this == other || (this == FullHistory && other == Recent)
}

// A sync time in the future means the clock moved back since, so it can't be trusted
internal fun isDue(lastSynced: Long?, now: Long, maxAgeMillis: Long): Boolean =
    lastSynced == null || lastSynced > now || now - lastSynced >= maxAgeMillis

internal fun needsFullHistory(plan: SyncPlan, range: Range): Boolean = when (plan) {
    SyncPlan.Whole -> false
    is SyncPlan.Recent -> range == Range.All || (range is Range.Last && range.months > plan.months)
}

/**
 * Null when the cache is fresh enough for this read. The full history syncs again every [fullHistoryMaxAgeMillis],
 * for older documents edited on another phone, which neither the recent sync nor the count check sees.
 */
internal fun syncWork(plan: SyncPlan, range: Range, status: SyncStatus, now: Long, maxAgeMillis: Long, fullHistoryMaxAgeMillis: Long): SyncWork? = when {
    plan == SyncPlan.Whole -> SyncWork.Whole.takeIf { isDue(status.lastSynced, now, maxAgeMillis) }
    needsFullHistory(plan, range) && isDue(status.fullHistorySynced, now, fullHistoryMaxAgeMillis) -> SyncWork.FullHistory
    isDue(status.lastSynced, now, maxAgeMillis) -> SyncWork.Recent
    else -> null
}

// Whether the cache has been synced as far back as this read goes
internal fun covers(status: SyncStatus, plan: SyncPlan, range: Range): Boolean =
    if (needsFullHistory(plan, range)) status.fullHistorySynced != null else status.lastSynced != null

/**
 * The cache once it's covered. Before that, what's cached when there's any, unless the read needs the full
 * history, which a part of would show wrong totals. Otherwise the shimmer, until a sync fails.
 *
 * @param cacheBehind The cache hasn't shown anything since the last sync finished. Firestore can report a
 * read done before its listeners show what it brought, so an empty cache then isn't the server's answer.
 */
internal fun <T> loadOf(cached: T, isEmpty: Boolean, covered: Boolean, needsFullHistory: Boolean, status: SyncStatus, cacheBehind: Boolean = false): Load<T> = when {
    covered && isEmpty && cacheBehind && status.lastFetched > 0 -> Load.Loading
    covered -> Load.Ready(cached)
    !isEmpty && !needsFullHistory -> Load.Ready(cached)
    status.lastFailure != null && !status.syncing -> Load.Unavailable(status.lastFailure)
    else -> Load.Loading
}

// Calendar months back from [now], in the device's time zone
internal fun windowStart(now: Long, months: Int): Long = Calendar.getInstance().run {
    timeInMillis = now
    add(Calendar.MONTH, -months)
    timeInMillis
}

// A missing or invalid value falls back to the default. Debug builds too: a full history on every read is too much.
internal fun fullHistoryMaxAgeMillis(remoteDays: Long?, defaultDays: Long): Long =
    TimeUnit.DAYS.toMillis(if (remoteDays == null || remoteDays <= 0) defaultDays else remoteDays)

// Debug builds sync on every read, like Remote Config. A missing or invalid value falls back to the default.
internal fun maxAgeMillis(debug: Boolean, remoteMinutes: Long?, defaultMinutes: Long): Long = when {
    debug -> 0
    remoteMinutes == null || remoteMinutes < 0 -> TimeUnit.MINUTES.toMillis(defaultMinutes)
    else -> TimeUnit.MINUTES.toMillis(remoteMinutes)
}
