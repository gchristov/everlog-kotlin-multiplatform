package com.everlog.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.everlog.constants.ELConstants
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class NetworkUtils {

    companion object {

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
                val url = URL(ELConstants.URL_PING)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = ELConstants.NETWORK_PING_TIMEOUT_INTERVAL
                connection.connect()
                success = connection.responseCode == 200
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return success
        }
    }
}