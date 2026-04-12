package com.everlog.managers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationManagerCompat
import com.everlog.application.ELApplication
import com.everlog.utils.device.DeviceUtils
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
            if (DeviceUtils.isAndroidU()) {
                service.startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
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
            val manager = NotificationManagerCompat.from(ELApplication.getInstance())
            manager.notify(notificationId, notification)
            Timber.tag(TAG).d("Notified user with notification")
        }

        @JvmStatic
        fun cancel(notificationId: Int) {
            val manager = NotificationManagerCompat.from(ELApplication.getInstance())
            manager.cancel(notificationId)
            Timber.tag(TAG).d("Cancelled notification")
        }

        private fun createNotificationChannel(options: NotificationChannelOptions) {
            if (DeviceUtils.isAndroidO()) {
                val manager = NotificationManagerCompat.from(ELApplication.getInstance())
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