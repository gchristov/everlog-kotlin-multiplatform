package com.everlog.android.common.kotlin.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.ServiceInfo
import com.everlog.android.common.kotlin.AppContext
import android.os.Build
import timber.log.Timber

class ELNotificationManager {

    companion object {

        private const val TAG = "ELNotificationManager"

        @JvmStatic
        fun startForeground(service: Service,
                            notificationId: Int,
                            notification: Notification,
                            channelOptions: NotificationChannelOptions) {
            // Make sure we have a channel
            createNotificationChannel(channelOptions)
            // Then start service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                service.startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                service.startForeground(notificationId, notification)
            }
        }

        @JvmStatic
        fun notify(notificationId: Int,
                   notification: Notification,
                   channelOptions: NotificationChannelOptions) {
            // Make sure we have a channel
            createNotificationChannel(channelOptions)
            // Then show notification
            val manager = AppContext.getSystemService(NotificationManager::class.java)
            manager.notify(notificationId, notification)
            Timber.tag(TAG).d("Notified user with notification")
        }

        @JvmStatic
        fun cancel(notificationId: Int) {
            val manager = AppContext.getSystemService(NotificationManager::class.java)
            manager.cancel(notificationId)
            Timber.tag(TAG).d("Cancelled notification")
        }

        private fun createNotificationChannel(options: NotificationChannelOptions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = AppContext.getSystemService(NotificationManager::class.java)
                val channel = NotificationChannel(options.id, options.name, if (options.important) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT)
                channel.description = options.description
                if (options.disableSound) {
                    channel.setSound(null, null)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    data class NotificationChannelOptions (
            var id: String,
            var name: String,
            var description: String,
            var important: Boolean,
            var disableSound: Boolean)
}