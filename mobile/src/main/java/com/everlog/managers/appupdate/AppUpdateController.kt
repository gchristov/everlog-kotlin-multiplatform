package com.everlog.managers.appupdate

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.everlog.managers.apprate.AppLaunchManager
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import rx.Emitter
import rx.Observable

/**
 * Wraps the Play In-App Updates API for the flexible update flow: Play shows its own prompt and
 * downloads the update in the background, then the app asks the user to restart to install it.
 *
 * Only works for builds installed from Play; anywhere else (e.g. debug builds) no update is ever
 * reported.
 */
class AppUpdateController(context: Context) {

    private val mAppUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)
    private var mInstallStateListener: InstallStateUpdatedListener? = null

    fun fetchUpdateInfo(): Observable<AppUpdateInfo> {
        return Observable.create({ emitter ->
            mAppUpdateManager.appUpdateInfo
                    .addOnSuccessListener { info ->
                        emitter.onNext(info)
                        emitter.onCompleted()
                    }
                    .addOnFailureListener { emitter.onError(it) }
        }, Emitter.BackpressureMode.LATEST)
    }

    fun isUpdateDownloaded(info: AppUpdateInfo): Boolean {
        return info.installStatus() == InstallStatus.DOWNLOADED
    }

    fun shouldPromptUpdate(info: AppUpdateInfo): Boolean {
        return info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                && !isUpdateInProgress(info)
                && AppLaunchManager.manager.shouldPromptAppUpdate(info.availableVersionCode())
    }

    private fun isUpdateInProgress(info: AppUpdateInfo): Boolean {
        // Already accepted, so the update is still reported as available while it downloads/installs
        return when (info.installStatus()) {
            InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.DOWNLOADED, InstallStatus.INSTALLING -> true
            else -> false
        }
    }

    fun startFlexibleUpdate(info: AppUpdateInfo, launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        return mAppUpdateManager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE))
    }

    fun updateDeclined(info: AppUpdateInfo) {
        AppLaunchManager.manager.appUpdateDeclined(info.availableVersionCode())
    }

    /**
     * Installs a downloaded update. This restarts the app.
     */
    fun completeUpdate() {
        mAppUpdateManager.completeUpdate()
    }

    fun registerInstallListener(onDownloaded: () -> Unit, onFailed: (errorCode: Int) -> Unit) {
        unregisterInstallListener()
        val listener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> onDownloaded()
                InstallStatus.FAILED -> onFailed(state.installErrorCode())
                else -> {
                    // No-op
                }
            }
        }
        mAppUpdateManager.registerListener(listener)
        mInstallStateListener = listener
    }

    fun unregisterInstallListener() {
        mInstallStateListener?.let { mAppUpdateManager.unregisterListener(it) }
        mInstallStateListener = null
    }
}
