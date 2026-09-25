package com.everlog.managers.appupdate

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.everlog.managers.appupdate.AppUpdateSession.Action
import com.everlog.managers.appupdate.AppUpdateSession.Outcome
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import timber.log.Timber
import java.util.Date

/**
 * Runs the Play In-App Updates flexible flow: Play shows its own prompt and downloads the update
 * in the background, then [onReadyToInstall] asks the screen to offer a restart, which calls
 * [completeUpdate].
 *
 * Only works for builds installed from Play; anywhere else (e.g. debug builds) no update is ever
 * reported.
 */
class AppUpdateController(
    context: Context,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>,
    private val onReadyToInstall: () -> Unit
) {

    companion object {
        private const val TAG = "AppUpdateController"
    }

    private val mAppUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)
    private val mInstallStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                AnalyticsManager.manager.appUpdateDownloaded()
                onReadyToInstall()
            }
            InstallStatus.FAILED -> {
                mSession.downloadStopped()
                AnalyticsManager.manager.appUpdateFailed(state.installErrorCode())
            }
            InstallStatus.CANCELED -> {
                mSession.downloadStopped()
            }
            else -> {
                // No-op
            }
        }
    }
    private val mSession = AppUpdateSession { versionCode ->
        AppLaunchManager.manager.shouldPromptAppUpdate(versionCode, Date())
    }
    private var mReleased = false

    init {
        mAppUpdateManager.registerListener(mInstallStateListener)
    }

    /**
     * Offers a restart for an update that's already downloaded, or prompts for a new one.
     */
    fun checkForUpdate() {
        mAppUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    if (mReleased) {
                        return@addOnSuccessListener
                    }
                    when (mSession.check(toUpdate(info))) {
                        // Downloaded while the app was in the background or on another screen
                        Action.OFFER_RESTART -> onReadyToInstall()
                        Action.PROMPT -> prompt(info)
                        Action.NONE -> {
                            // No-op
                        }
                    }
                }
                .addOnFailureListener {
                    // Expected when not installed from Play (e.g. debug builds), so don't report it as an error
                    Timber.tag(TAG).w("App update check failed: %s", it.message)
                }
    }

    fun onUpdateFlowResult(resultCode: Int) {
        val result = mSession.flowResult(resultCode) ?: return
        when (result.outcome) {
            Outcome.ACCEPTED -> {
                AnalyticsManager.manager.appUpdateAccepted(result.versionCode)
            }
            Outcome.DECLINED -> {
                AppLaunchManager.manager.appUpdateDeclined(result.versionCode, Date())
                AnalyticsManager.manager.appUpdateDeclined(result.versionCode)
            }
            Outcome.FAILED -> {
                AnalyticsManager.manager.appUpdateFailed(resultCode)
            }
        }
    }

    /**
     * Installs a downloaded update. This restarts the app, so only a failure comes back, as
     * another [onReadyToInstall] to retry.
     */
    fun completeUpdate() {
        AnalyticsManager.manager.appUpdateRestartTapped()
        mAppUpdateManager.completeUpdate()
                .addOnFailureListener {
                    Timber.tag(TAG).w(it, "App update install failed")
                    AnalyticsManager.manager.appUpdateFailed((it as? InstallException)?.errorCode ?: 0)
                    if (!mReleased) {
                        onReadyToInstall()
                    }
                }
    }

    fun release() {
        mReleased = true
        mAppUpdateManager.unregisterListener(mInstallStateListener)
    }

    private fun toUpdate(info: AppUpdateInfo): AppUpdateSession.Update {
        return AppUpdateSession.Update(
            versionCode = info.availableVersionCode(),
            availability = info.updateAvailability(),
            flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
            installStatus = info.installStatus()
        )
    }

    private fun prompt(info: AppUpdateInfo) {
        if (mAppUpdateManager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE))) {
            mSession.promptShown(info.availableVersionCode())
            AnalyticsManager.manager.appUpdatePromptShown(info.availableVersionCode())
        } else {
            Timber.tag(TAG).w("App update flow not started: versionCode=%s", info.availableVersionCode())
        }
    }
}
