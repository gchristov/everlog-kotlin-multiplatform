package com.everlog.config

import androidx.core.content.ContextCompat
import com.everlog.BuildConfig
import com.everlog.R
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.session.SessionDefaults
import com.everlog.managers.ErrorManager
import com.everlog.managers.RemoteConfigManager
import com.everlog.managers.analytics.AnalyticsManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

class AppConfig {

    val defaultRestTimeSeconds = ELExerciseGroup.DEFAULT_REST_TIME_SECONDS
    val defaultExerciseTimeSeconds = SessionDefaults.EXERCISE_TIME_SECONDS

    val maxExerciseSets = SessionDefaults.MAX_EXERCISE_SETS

    // Qualifying actions needed before each rating prompt, from any trigger. Its size caps the prompts.
    val ratePromptActionGoals = listOf(3, 3, 4)
    // Play doesn't publish its quota. Asking more often likely gets silently skipped, wasting a prompt.
    val ratePromptCooldownDays = 30
    // Shorter workouts are likely tests or accidental saves
    val ratePromptMinWorkoutMinutes = 5

    val appUpdateRepromptDelayDays = 7

    // All-time history is read from the cache, so nothing may be evicted: Firestore's default is 100 MB,
    // after which it drops the least recently used documents
    val firestoreCacheSizeBytes = 1024L * 1024 * 1024

    // Per server read: a weak signal shows the cache, or Unavailable when nothing is cached, rather than loading for long
    val dataSyncTimeoutSeconds = 20L
    // Workouts per server read when syncing a full history
    val dataSyncChunkSize = 200
    // The workouts each due sync reads again: the 1RM window set suggestions use (90 days). Older ones sync with the full history, weekly.
    val workoutSyncMonths = 3

    companion object {

        @JvmField
        val configuration = AppConfig()

        // Remote Config's data_sync_max_age_minutes when it has none
        const val DATA_SYNC_MAX_AGE_MINUTES = 60L

        // Remote Config's data_sync_full_history_max_age_days when it has none
        const val DATA_SYNC_FULL_HISTORY_MAX_AGE_DAYS = 7L
    }

    fun configureApp() {
        configureFirestore()
        configureAnalyticsManager()
        configureErrorManager()
        refreshRemoteConfig()
    }

    // Settings only apply before Firestore's first use, so this runs first
    private fun configureFirestore() {
        FirebaseFirestore.setLoggingEnabled(BuildConfig.DEBUG)
        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { setSizeBytes(firestoreCacheSizeBytes) })
        }
    }

    private fun configureErrorManager() {
        ErrorManager.manager.initialize()
    }

    // Screens opened before Home, like onboarding's, read the flags too, so they can't wait for Home's refresh
    private fun refreshRemoteConfig() {
        RemoteConfigManager.manager.refreshAppConfig()
    }

    private fun configureAnalyticsManager() {
        AnalyticsManager.manager.initialize()
    }
}