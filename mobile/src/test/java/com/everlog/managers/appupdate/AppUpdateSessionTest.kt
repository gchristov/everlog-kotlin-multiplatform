package com.everlog.managers.appupdate

import android.app.Activity
import com.everlog.managers.appupdate.AppUpdateSession.Action
import com.everlog.managers.appupdate.AppUpdateSession.FlowResult
import com.everlog.managers.appupdate.AppUpdateSession.Outcome
import com.everlog.managers.appupdate.AppUpdateSession.Update
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppUpdateSessionTest {

    // ActivityResult.RESULT_IN_APP_UPDATE_FAILED
    private val resultInAppUpdateFailed = 1

    private val versionCode = 1_020_300

    private val cooledDownVersionCodes = mutableSetOf<Int>()
    private val session = AppUpdateSession { it !in cooledDownVersionCodes }

    // Checking

    @Test
    fun `prompts for an available update`() {
        assertThat(session.check(available())).isEqualTo(Action.PROMPT)
    }

    @Test
    fun `does nothing when no update is available`() {
        assertThat(session.check(available(availability = UpdateAvailability.UPDATE_NOT_AVAILABLE))).isEqualTo(Action.NONE)
    }

    @Test
    fun `does nothing when Play doesn't allow a flexible update`() {
        assertThat(session.check(available(flexibleAllowed = false))).isEqualTo(Action.NONE)
    }

    @Test
    fun `does nothing while a declined version is cooling down`() {
        cooledDownVersionCodes += versionCode

        assertThat(session.check(available())).isEqualTo(Action.NONE)
    }

    @Test
    fun `does nothing while an update is downloading or installing`() {
        listOf(InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.INSTALLING).forEach { status ->
            assertThat(session.check(available(installStatus = status))).isEqualTo(Action.NONE)
        }
    }

    @Test
    fun `offers a restart for a downloaded update`() {
        assertThat(session.check(available(installStatus = InstallStatus.DOWNLOADED))).isEqualTo(Action.OFFER_RESTART)
    }

    @Test
    fun `offers a restart for a downloaded update even while its version is cooling down`() {
        cooledDownVersionCodes += versionCode

        assertThat(session.check(available(installStatus = InstallStatus.DOWNLOADED))).isEqualTo(Action.OFFER_RESTART)
    }

    @Test
    fun `doesn't prompt again while the prompt is showing`() {
        session.promptShown(versionCode)

        assertThat(session.check(available())).isEqualTo(Action.NONE)
    }

    // Accepting

    @Test
    fun `accepting reports the prompted version`() {
        session.promptShown(versionCode)

        assertThat(session.flowResult(Activity.RESULT_OK)).isEqualTo(FlowResult(Outcome.ACCEPTED, versionCode))
    }

    @Test
    fun `doesn't prompt again for an accepted update Play still reports as available`() {
        session.promptShown(versionCode)
        session.flowResult(Activity.RESULT_OK)

        // Play hasn't registered the download yet
        assertThat(session.check(available())).isEqualTo(Action.NONE)
    }

    @Test
    fun `prompts for a newer version than the accepted one`() {
        session.promptShown(versionCode)
        session.flowResult(Activity.RESULT_OK)

        assertThat(session.check(available(versionCode = versionCode + 1))).isEqualTo(Action.PROMPT)
    }

    @Test
    fun `prompts again once an accepted download fails`() {
        session.promptShown(versionCode)
        session.flowResult(Activity.RESULT_OK)

        session.downloadFailed()

        assertThat(session.check(available())).isEqualTo(Action.PROMPT)
    }

    @Test
    fun `cancelling an accepted download reports it to be declined`() {
        session.promptShown(versionCode)
        session.flowResult(Activity.RESULT_OK)

        assertThat(session.downloadCancelled()).isEqualTo(versionCode)
    }

    @Test
    fun `cancelling a download not accepted in this session reports nothing`() {
        assertThat(session.downloadCancelled()).isNull()
    }

    @Test
    fun `cancelling an accepted download leaves prompting to the decline cooldown`() {
        session.promptShown(versionCode)
        session.flowResult(Activity.RESULT_OK)

        // The controller records the decline, which starts the cooldown
        cooledDownVersionCodes += session.downloadCancelled()!!

        assertThat(session.check(available())).isEqualTo(Action.NONE)
    }

    // Declining and failing

    @Test
    fun `dismissing the prompt reports a decline`() {
        session.promptShown(versionCode)

        assertThat(session.flowResult(Activity.RESULT_CANCELED)).isEqualTo(FlowResult(Outcome.DECLINED, versionCode))
    }

    @Test
    fun `a failed prompt reports a failure and can prompt again`() {
        session.promptShown(versionCode)

        assertThat(session.flowResult(resultInAppUpdateFailed)).isEqualTo(FlowResult(Outcome.FAILED, versionCode))
        assertThat(session.check(available())).isEqualTo(Action.PROMPT)
    }

    @Test
    fun `ignores a result when nothing was prompted`() {
        assertThat(session.flowResult(Activity.RESULT_CANCELED)).isNull()
    }

    @Test
    fun `ignores a second result for the same prompt`() {
        session.promptShown(versionCode)
        session.flowResult(Activity.RESULT_OK)

        assertThat(session.flowResult(Activity.RESULT_CANCELED)).isNull()
    }

    // Helpers

    private fun available(
        versionCode: Int = this.versionCode,
        availability: Int = UpdateAvailability.UPDATE_AVAILABLE,
        flexibleAllowed: Boolean = true,
        installStatus: Int = InstallStatus.UNKNOWN
    ) = Update(versionCode, availability, flexibleAllowed, installStatus)
}
