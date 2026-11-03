package com.everlog.data.sync

import android.content.Context
import com.everlog.BuildConfig
import com.everlog.android.datasync.DataSync
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.android.datasync.SyncTimeoutException
import com.everlog.android.datasync.SyncReport
import com.everlog.android.datasync.PreferencesSyncRecords
import com.everlog.android.datasync.SyncSettings
import com.everlog.android.datasync.olderMaxAgeMillis
import com.everlog.android.datasync.maxAgeMillis
import com.everlog.android.common.kotlin.AppContext
import com.everlog.config.AppConfig
import com.everlog.managers.RemoteConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit

// The app's one DataSync. Its syncs run in their own scope, so one finishes after the screen that started it closes.
object AppDataSync {

    // A single document's source name ends in its id, e.g. "globalExercise/<uuid>", left out so they count together
    private fun reportSync(report: SyncReport) = AnalyticsManager.manager.dataSynced(
        collection = report.source.substringBefore('/'),
        work = report.work,
        documents = report.documents,
        durationMillis = report.durationMillis,
        outcome = when (report.failure) {
            null -> "synced"
            is SyncTimeoutException -> "timed_out"
            else -> "failed"
        },
    )

    val instance: DataSync by lazy {
        val config = AppConfig.configuration
        DataSync(
            records = PreferencesSyncRecords(AppContext.getSharedPreferences(PreferencesSyncRecords.FILE, Context.MODE_PRIVATE)),
            settings = SyncSettings(
                maxAgeMillis = { maxAgeMillis(BuildConfig.DEBUG, RemoteConfigManager.manager.dataSyncMaxAgeMinutes(), AppConfig.DATA_SYNC_MAX_AGE_MINUTES) },
                olderMaxAgeMillis = {
                    olderMaxAgeMillis(RemoteConfigManager.manager.dataSyncOlderMaxAgeDays(), AppConfig.DATA_SYNC_OLDER_MAX_AGE_DAYS)
                },
                timeoutMillis = TimeUnit.SECONDS.toMillis(config.dataSyncTimeoutSeconds),
                chunkSize = config.dataSyncChunkSize,
            ),
            clock = System::currentTimeMillis,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            report = ::reportSync,
        )
    }
}
