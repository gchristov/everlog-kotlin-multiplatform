package com.everlog.android.plan.data.cover

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class NetworkUtils {

    companion object {
        private const val PING_URL = "https://google.com"
        private const val PING_TIMEOUT_MILLIS = 5000

        fun hasNetwork(context: Context): Boolean {
            var isConnected = false
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
            if (activeNetwork != null && activeNetwork.isConnected) {
                isConnected = true
            }
            return isConnected
        }

        fun isConnected(): Boolean {
            var success = false
            try {
                val url = URL(PING_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = PING_TIMEOUT_MILLIS
                connection.connect()
                success = connection.responseCode == 200
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return success
        }
    }
}