package com.everlog.android.template.data.session

import com.everlog.android.common.data.model.ELRoutine

/**
 * The plan the user is following, which is kept on the phone with a copy of each of its routines.
 */
interface OngoingPlanRepository {
    /**
     * Puts [routine] in place of the plan's copy of it, if there's an ongoing plan and the routine is in it.
     */
    fun replaceRoutine(routine: ELRoutine)
}
