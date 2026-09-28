package com.everlog.managers.apprate

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
 *
 * If the screen goes away or gets covered before the dialog launches, nothing is shown and no prompt
 * is used up. The goal stays met, so the next qualifying action prompts.
 */
class RatePromptController @JvmOverloads constructor(
    private val activity: AppCompatActivity,
    private val reviewManager: ReviewManager = createReviewManager(activity)
) {

    companion object {
        private const val TAG = "RatePromptController"

        // Process-wide, so two triggers at the same moment can't both launch the flow. Holds the
        // controller running the flow, so one that's finished can't release a newer one's.
        private var inFlight: RatePromptController? = null

        private fun createReviewManager(activity: AppCompatActivity): ReviewManager {
            val context = activity.applicationContext
            return if (BuildConfig.DEBUG) FakeReviewManager(context) else ReviewManagerFactory.create(context)
        }

        /**
         * Whether the screen is still showing and nothing covers it, like a dialog or share sheet.
         */
        @VisibleForTesting
        internal fun canLaunch(state: Lifecycle.State, isFinishing: Boolean, hasWindowFocus: Boolean): Boolean {
            return state.isAtLeast(Lifecycle.State.RESUMED) && !isFinishing && hasWindowFocus
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            release()
        }
    }

    /**
     * Launches the dialog [delayMillis] from now, if [trigger] qualifies and the user is due a prompt.
     */
    fun request(trigger: RatePromptTrigger, delayMillis: Long) {
        if (!trigger.isEligible()) {
            return
        }
        // Counts even if the screen is already gone, as the user still did the thing
        val due = RatePromptManager.manager.recordAction(trigger.actionId, System.currentTimeMillis())
        if (!due || inFlight != null || activity.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            return
        }
        inFlight = this
        activity.lifecycle.addObserver(lifecycleObserver)
        val launchAt = SystemClock.uptimeMillis() + delayMillis
        // Fetched during the delay, as it can take a moment
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (inFlight !== this) {
                // Screen was destroyed while fetching
                return@addOnCompleteListener
            }
            if (task.isSuccessful) {
                handler.postAtTime({ launch(trigger, task.result) }, launchAt)
            } else {
                // Expected without Play (e.g. not installed from Play), so a log rather than a non-fatal
                Timber.tag(TAG).w("Review flow request failed: source=%s error=%s", trigger.source, task.exception)
                release()
            }
        }
    }

    private fun launch(trigger: RatePromptTrigger, reviewInfo: ReviewInfo) {
        if (inFlight !== this) {
            return
        }
        if (!canLaunch(activity.lifecycle.currentState, activity.isFinishing, activity.hasWindowFocus())) {
            Timber.tag(TAG).i("Screen left or covered, not launching: source=%s", trigger.source)
            release()
            return
        }
        val promptNumber = RatePromptManager.manager.promptLaunched(System.currentTimeMillis())
        AnalyticsManager.manager.ratePromptTriggered(trigger.source, promptNumber)
        if (BuildConfig.DEBUG) {
            ToastBuilder.showToast(activity, "Rating prompt $promptNumber launched (${trigger.source})")
        }
        reviewManager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener {
            release()
        }
    }

    private fun release() {
        handler.removeCallbacksAndMessages(null)
        activity.lifecycle.removeObserver(lifecycleObserver)
        if (inFlight === this) {
            inFlight = null
        }
    }
}
