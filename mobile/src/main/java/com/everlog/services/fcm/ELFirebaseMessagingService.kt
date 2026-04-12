package com.everlog.services.fcm

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import com.everlog.R
import com.everlog.application.ELApplication
import com.everlog.managers.ELNotificationManager.Companion.notify
import com.everlog.managers.ELNotificationManager.NotificationChannelOptions
import com.everlog.ui.activities.splash.SplashActivity
import com.everlog.utils.FCMUtils
import com.everlog.utils.device.DeviceUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber
import kotlin.random.Random

class ELFirebaseMessagingService : FirebaseMessagingService() {

    val TAG = "ELFirebaseMessagingSvc"
    val NOTIFICATION_ID = 102

    companion object {

        @JvmStatic
        fun notify(notificationId: Int,
                   title: String,
                   body: String) {
            notify(notificationId,
                    buildNotification(title, body),
                    NotificationChannelOptions(notificationChannelId(),
                            "Notifications",
                            "Receive updates from Everlog.",
                            important = false,
                            disableSound = false))
        }

        private fun buildNotification(title: String, message: String): Notification {
            val builder = NotificationCompat.Builder(ELApplication.getInstance(), notificationChannelId())
            builder
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle())
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(buildOpenAppPendingIntent())
            if (!DeviceUtils.isAndroidO()) {
                builder.priority = Notification.PRIORITY_DEFAULT
            }
            return builder.build()
        }

        private fun buildOpenAppPendingIntent(): PendingIntent {
            val intent = Intent(ELApplication.getInstance(), SplashActivity::class.java)
            return PendingIntent.getActivity(ELApplication.getInstance(), Random.nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        }

        private fun notificationChannelId(): String {
            return ELApplication.getInstance().getString(R.string.notification_channel_normal)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FCMUtils.refreshTokenForLoggedInUser(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.tag(TAG).i("Received notification")
        val notification = remoteMessage.notification
        if (notification != null) {
            val title = notification.title
            val body = notification.body
            Timber.tag(TAG).d("Notification contents: title=$title body=$body")
            if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(body)) {
                Timber.tag(TAG).i("Showing notification")
                notify(NOTIFICATION_ID, title!!, body!!)
            }
        }
    }
}