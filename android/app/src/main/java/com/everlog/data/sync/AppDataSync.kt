package com.everlog.data.sync

import android.content.Context
import com.everlog.BuildConfig
import com.everlog.android.common.datasync.DataSync
import com.everlog.android.common.datasync.PreferencesSyncRecords
import com.everlog.android.common.datasync.SyncSettings
import com.everlog.android.common.datasync.fullHistoryMaxAgeMillis
import com.everlog.android.common.datasync.maxAgeMillis
import com.everlog.android.common.kotlin.AppContext
import com.everlog.config.AppConfig
import com.everlog.managers.RemoteConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit

// The app's one DataSync. Its syncs run in their own scope, so one finishes after the screen that started it closes.
object AppDataSync {

    val instance: DataSync by lazy {
        val config = AppConfig.configuration
        DataSync(
            records = PreferencesSyncRecords(AppContext.getSharedPreferences(PreferencesSyncRecords.FILE, Context.MODE_PRIVATE)),
            settings = SyncSettings(
                maxAgeMillis = { maxAgeMillis(BuildConfig.DEBUG, RemoteConfigManager.manager.dataSyncMaxAgeMinutes(), AppConfig.DATA_SYNC_MAX_AGE_MINUTES) },
                fullHistoryMaxAgeMillis = {
                    fullHistoryMaxAgeMillis(RemoteConfigManager.manager.dataSyncFullHistoryMaxAgeDays(), AppConfig.DATA_SYNC_FULL_HISTORY_MAX_AGE_DAYS)
                },
                timeoutMillis = TimeUnit.SECONDS.toMillis(config.dataSyncTimeoutSeconds),
                chunkSize = config.dataSyncChunkSize,
            ),
            clock = System::currentTimeMillis,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }
}
