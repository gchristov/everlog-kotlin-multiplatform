package com.everlog.logging

import android.util.Log

class LogFormatter {

    companion object {

        @JvmStatic
        fun format(priority: Int, tag: String?, message: String?): String {
            val messageWithTag = if (tag != null) "[$tag] $message" else message
            return prefixForPriority(priority) + messageWithTag
        }

        private fun prefixForPriority(priority: Int): String {
            return when (priority) {
                Log.VERBOSE -> "[VERBOSE] "
                Log.DEBUG -> "[DEBUG] "
                Log.INFO -> "[INFO] "
                Log.WARN -> "[WARN] "
                Log.ERROR -> "[ERROR] "
                Log.ASSERT -> "[ASSERT] "
                else -> "[UNKNOWN($priority)] "
            }
        }
    }
}