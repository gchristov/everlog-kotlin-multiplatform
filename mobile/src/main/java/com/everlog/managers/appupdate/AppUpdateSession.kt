package com.everlog.managers.appupdate

import android.app.Activity
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * The in-app update decisions for one screen session, kept free of Play and Android calls so they
 * can be unit tested. [AppUpdateController] feeds it what Play reports and acts on what it returns.
 *
 * @param canPromptVersion whether the user can be asked about a version again, i.e. it isn't
 * within the cooldown after they declined it.
 */
class AppUpdateSession(private val canPromptVersion: (versionCode: Int) -> Boolean) {

    /**
     * What Play reports about an update, from its AppUpdateInfo.
     */
    data class Update(
        val versionCode: Int,
        val availability: Int,
        val flexibleAllowed: Boolean,
        val installStatus: Int
    )

    enum class Action {
        NONE,
        PROMPT,
        OFFER_RESTART
    }

    enum class Outcome {
        ACCEPTED,
        DECLINED,
        FAILED
    }

    data class FlowResult(val outcome: Outcome, val versionCode: Int)

    // The update currently offered in Play's prompt, to match against the flow result
    private var mPromptedVersionCode: Int? = null
    // Play can briefly keep reporting an accepted update as available before its download starts
    private var mAcceptedVersionCode: Int? = null

    fun check(update: Update): Action {
        return when {
            update.installStatus == InstallStatus.DOWNLOADED -> Action.OFFER_RESTART
            shouldPrompt(update) -> Action.PROMPT
            else -> Action.NONE
        }
    }

    fun promptShown(versionCode: Int) {
        mPromptedVersionCode = versionCode
    }

    /**
     * Matches Play's prompt result to the prompted version, or returns null if nothing was prompted.
     */
    fun flowResult(resultCode: Int): FlowResult? {
        val versionCode = mPromptedVersionCode ?: return null
        mPromptedVersionCode = null
        val outcome = when (resultCode) {
            Activity.RESULT_OK -> {
                mAcceptedVersionCode = versionCode
                Outcome.ACCEPTED
            }
            Activity.RESULT_CANCELED -> Outcome.DECLINED
            // ActivityResult.RESULT_IN_APP_UPDATE_FAILED; the next check will offer it again
            else -> Outcome.FAILED
        }
        return FlowResult(outcome, versionCode)
    }

    /**
     * An accepted download failed, so let the next check offer the update again.
     */
    fun downloadFailed() {
        mAcceptedVersionCode = null
    }

    /**
     * The user cancelled an accepted download from Play's notification, which counts as declining
     * it. Returns the cancelled version, or null if it wasn't accepted in this session.
     */
    fun downloadCancelled(): Int? {
        val versionCode = mAcceptedVersionCode
        mAcceptedVersionCode = null
        return versionCode
    }

    private fun shouldPrompt(update: Update): Boolean {
        return mPromptedVersionCode == null
                && update.versionCode != mAcceptedVersionCode
                && update.availability == UpdateAvailability.UPDATE_AVAILABLE
                && update.flexibleAllowed
                && !isInProgress(update.installStatus)
                && canPromptVersion(update.versionCode)
    }

    private fun isInProgress(installStatus: Int): Boolean {
        // Already accepted, so the update is still reported as available while it downloads/installs
        return when (installStatus) {
            InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.DOWNLOADED, InstallStatus.INSTALLING -> true
            else -> false
        }
    }
}
