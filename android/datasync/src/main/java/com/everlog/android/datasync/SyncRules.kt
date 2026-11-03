package com.everlog.android.datasync

import com.everlog.android.common.kotlin.Load
import java.util.Calendar
import java.util.concurrent.TimeUnit

// What a sync does
internal sealed interface SyncWork {
    // The whole collection or document, in one read
    data object Whole : SyncWork

    /**
     * The last months of a SyncPlan.Recent collection. With [backTo], also the documents older than that, back to
     * [backTo] by the plan's date field, in pages. Without it, it checks the counts of the part synced before.
     */
    data class Recent(val backTo: Long? = null) : SyncWork

    // A sync running [this] makes asking for [other] unnecessary
    fun covers(other: SyncWork): Boolean = when {
        this == other -> true
        this is Recent && other is Recent -> other.backTo == null || (backTo != null && backTo <= other.backTo)
        else -> false
    }
}

// A sync time in the future means the clock moved back since, so it can't be trusted
internal fun isDue(lastSynced: Long?, now: Long, maxAgeMillis: Long): Boolean =
    lastSynced == null || lastSynced > now || now - lastSynced >= maxAgeMillis

/**
 * How far back by the plan's date field a read from [from] needs the cache synced beyond the recent window, or null
 * when the window covers it.
 */
internal fun reachesBack(plan: SyncPlan, from: Long?, now: Long): Long? = when (plan) {
    SyncPlan.Whole -> null
    is SyncPlan.Recent -> from?.takeIf { it < windowStart(now, plan.months) }
}

/**
 * Null when the cache is fresh enough for this read. The part older than the window syncs again every
 * [olderMaxAgeMillis], for documents edited on another phone, which neither the recent sync nor the count check
 * sees.
 *
 * @param backTo What [reachesBack] says the read needs
 */
internal fun syncWork(plan: SyncPlan, backTo: Long?, status: SyncStatus, now: Long, maxAgeMillis: Long, olderMaxAgeMillis: Long): SyncWork? = when {
    plan == SyncPlan.Whole -> SyncWork.Whole.takeIf { isDue(status.lastSynced, now, maxAgeMillis) }
    backTo != null && (status.syncedBackTo == null || status.syncedBackTo > backTo) -> SyncWork.Recent(backTo)
    // Only as far back as this read goes, so what's kept fresh doesn't grow with every read further back
    backTo != null && isDue(status.syncedBackAt, now, olderMaxAgeMillis) -> SyncWork.Recent(backTo)
    isDue(status.lastSynced, now, maxAgeMillis) -> SyncWork.Recent()
    else -> null
}

// Whether the cache has been synced as far back as this read goes
internal fun covers(status: SyncStatus, backTo: Long?): Boolean = when {
    status.lastSynced == null -> false
    backTo == null -> true
    else -> status.syncedBackTo != null && status.syncedBackTo <= backTo
}

/**
 * The cache once it's covered. Before that, what's cached when there's any, unless the read needs a complete
 * copy, e.g. a year of history, which a part of would show wrong totals. Otherwise the shimmer, until a sync fails.
 *
 * @param cacheBehind The cache hasn't shown anything since the last sync finished. Firestore can report a
 * read done before its listeners show what it brought, so an empty cache then isn't the server's answer.
 */
internal fun <T> loadOf(cached: T, isEmpty: Boolean, covered: Boolean, needsComplete: Boolean, status: SyncStatus, cacheBehind: Boolean = false): Load<T> = when {
    covered && isEmpty && cacheBehind && status.lastFetched > 0 -> Load.Loading
    covered -> Load.Ready(cached)
    !isEmpty && !needsComplete -> Load.Ready(cached)
    status.lastFailure != null && !status.syncing -> Load.Unavailable(status.lastFailure)
    else -> Load.Loading
}

/**
 * Whether the cache shows what the last sync brought. Firestore can report a read done before its listeners
 * show it, so right after a first sync the cache may still hold only part of the collection, e.g. the few
 * documents read one at a time, or the recent workouts without the rest of the history. [cachedDocuments]
 * counts ones that don't parse too, so a document that doesn't parse can't hold the read up. Once the cache
 * has shown anything since the sync, it's caught up, e.g. after a delete.
 */
internal fun caughtUp(cachedDocuments: Int, status: SyncStatus, cacheBehind: Boolean): Boolean =
    !cacheBehind || cachedDocuments >= status.lastFetched

// Calendar months back from [now], in the device's time zone
internal fun windowStart(now: Long, months: Int): Long = Calendar.getInstance().run {
    timeInMillis = now
    add(Calendar.MONTH, -months)
    timeInMillis
}

// A missing or invalid value falls back to the default. Debug builds too: the older part on every read is too much.
fun olderMaxAgeMillis(remoteDays: Long?, defaultDays: Long): Long =
    TimeUnit.DAYS.toMillis(if (remoteDays == null || remoteDays <= 0) defaultDays else remoteDays)

// Debug builds sync on every read, like Remote Config. A missing or invalid value falls back to the default.
fun maxAgeMillis(debug: Boolean, remoteMinutes: Long?, defaultMinutes: Long): Long = when {
    debug -> 0
    remoteMinutes == null || remoteMinutes < 0 -> TimeUnit.MINUTES.toMillis(defaultMinutes)
    else -> TimeUnit.MINUTES.toMillis(remoteMinutes)
}
