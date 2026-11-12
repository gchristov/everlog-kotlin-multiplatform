package com.everlog.application.features

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.AppContext
import com.everlog.android.common.kotlin.Utils
import com.everlog.android.home.screen.HomeAppUpdates
import com.everlog.android.home.screen.HomeServices
import com.everlog.android.workout.data.WorkoutManager
import com.everlog.application.ELApplication
import com.everlog.data.datastores.ELDatastore
import com.everlog.managers.ErrorManager
import com.everlog.managers.RemoteConfigManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.managers.appupdate.AppUpdateController
import com.everlog.ui.navigator.ELNavigator
import com.everlog.utils.FCMUtils
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppHomeServices : HomeServices {

    private val navigator get() = ELNavigator(AppContext)

    override fun ongoingWorkout(): ELWorkout? = WorkoutManager.manager.ongoingWorkout()

    override fun discardOngoingWorkout() {
        WorkoutManager.manager.clearOngoingWorkout()
        // Removes the notification if the process was killed mid-workout and Android hasn't restarted the service
        // yet, which removes it too
        navigator.stopWorkoutService()
    }

    override fun onStarted() {
        ELApplication.graph.userSession.user()?.id?.let { id ->
            AnalyticsManager.manager.userIdentify(id)
            ErrorManager.manager.userIdentify(id)
        }
        FCMUtils.refreshFCMToken()
        Utils.runInBackground {
            ELApplication.graph.exerciseImageStorage.resumePendingUploads()
            // Counts the launch, for the rating prompt
            AppLaunchManager.manager.launchApp()
        }
    }

    override fun onResumed() {
        navigator.cancelAppUseNotification()
        RemoteConfigManager.manager.refreshAppConfig()
    }

    override fun onPaused() {
        navigator.scheduleAppUseNotification()
    }

    override fun onDestroyed() {
        ELDatastore.destroy()
    }

    override fun appUpdates(context: Context, launcher: ActivityResultLauncher<IntentSenderRequest>, onReady: () -> Unit): HomeAppUpdates {
        val controller = AppUpdateController(context, launcher, onReady)
        return object : HomeAppUpdates {
            override fun check() = controller.checkForUpdate()

            override fun onFlowResult(resultCode: Int) = controller.onUpdateFlowResult(resultCode)

            override fun restart() = controller.completeUpdate()

            override fun release() = controller.release()
        }
    }
}
