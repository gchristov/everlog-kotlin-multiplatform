package com.everlog.ui.adapters.plan

import com.everlog.android.plan.data.model.ELPlanWeek
import com.everlog.config.AppConfig
import com.everlog.managers.auth.LocalUserManager

// Before a week's days are shown: without a signed-in user nothing is locked
fun ELPlanWeek.applyProLock() {
    lockDays(AppConfig.configuration.maxPlanWeekDaysFree, userIsPro = LocalUserManager.getUser()?.isPro() != false)
}
