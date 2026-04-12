package com.everlog.utils

class Throttler {

    private var mLastActionMillis = 0L
    fun canPerformAction(millis: Int): Boolean {
        if (System.currentTimeMillis() - mLastActionMillis >= millis) {
            mLastActionMillis = System.currentTimeMillis()
            return true
        }
        return false
    }
}