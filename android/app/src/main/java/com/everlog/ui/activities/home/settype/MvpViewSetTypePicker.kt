package com.everlog.ui.activities.home.settype

import com.everlog.android.common.mvp.activity.BaseActivityMvpView

interface MvpViewSetTypePicker : BaseActivityMvpView {

    fun getSelectedExercisesCount(): Int
}