package com.everlog.android.common.navigation

// A plan's details, by its uuid: the plan models live in plan:data, which navigation can't see
data class PlanDetails(val planUuid: String) : Destination

// The congratulations screen after finishing a plan
data object PlanCongratulations : Destination
