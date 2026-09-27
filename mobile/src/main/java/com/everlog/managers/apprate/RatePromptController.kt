package com.everlog.managers.apprate

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.everlog.BuildConfig
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.dialog.ToastBuilder
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import timber.log.Timber

/**
 * Shows Play's in-app review dialog for a [RatePromptTrigger], if the action qualifies and the user is
 * due a prompt (see [RatePromptManager]).
 *
 * Play decides whether the dialog actually appears and never says what the user did, so a prompt counts
 * as soon as it's launched. It only appears for builds installed from Play; debug builds use
 * [FakeReviewManager], which shows nothing, and a toast instead.
 */
class RatePromptController @JvmOverloads constructor(
    private val activity: AppCompatActivity,
    private val reviewManager: ReviewManager = createReviewManager(activity)
) {

    companion object {
        private const val TAG = "RatePromptController"

        // Process-wide, so two triggers at the same moment can't both launch the flow
        private var inFlight = false

        private fun createReviewManager(activity: AppCompatActivity): ReviewManager {
            val context = activity.applicationContext
            return if (BuildConfig.DEBUG) FakeReviewManager(context) else ReviewManagerFactory.create(context)
        }
    }

    /**
     * Launches the dialog [delayMillis] from now, if [trigger] qualifies and the user is due a prompt.
     */
    fun request(trigger: RatePromptTrigger, delayMillis: Long) {
        if (!trigger.isEligible()) {
            return
        }
        if (!RatePromptManager.manager.recordAction(System.currentTimeMillis()) || inFlight) {
            return
        }
        inFlight = true
        val launchAt = SystemClock.uptimeMillis() + delayMillis
        // Fetched during the delay, as it can take a moment
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Handler(Looper.getMainLooper()).postAtTime({ launch(trigger, task.result) }, launchAt)
            } else {
                Timber.tag(TAG).w(task.exception, "Review flow request failed: source=%s", trigger.source)
                inFlight = false
            }
        }
    }

    private fun launch(trigger: RatePromptTrigger, reviewInfo: ReviewInfo) {
        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            // User left the screen. The goal is still met, so the next qualifying action prompts.
            inFlight = false
            return
        }
        val promptNumber = RatePromptManager.manager.promptLaunched(System.currentTimeMillis())
        AnalyticsManager.manager.ratePromptTriggered(trigger.source, promptNumber)
        if (BuildConfig.DEBUG) {
            ToastBuilder.showToast(activity, "Rating prompt $promptNumber launched (${trigger.source})")
        }
        reviewManager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener {
            inFlight = false
        }
    }
}
