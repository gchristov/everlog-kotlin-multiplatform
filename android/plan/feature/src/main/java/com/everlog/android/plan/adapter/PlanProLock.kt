package com.everlog.android.plan.adapter

import com.everlog.android.plan.data.model.ELPlanWeek
import com.everlog.android.auth.data.LocalUserManager

// Before a week's days are shown: without a signed-in user nothing is locked
fun ELPlanWeek.applyProLock() {
    lockDays(MAX_FREE_WEEK_DAYS, userIsPro = LocalUserManager.getUser()?.isPro() != false)
}

// How many days of each week a free user can fill in
const val MAX_FREE_WEEK_DAYS = 1

// How faint a day locked behind Pro shows
const val PRO_LOCKED_ALPHA = 0.4f
