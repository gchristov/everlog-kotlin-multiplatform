package com.everlog.android.template.feature.settype

import com.everlog.android.common.mvp.activity.BaseActivityMvpView

interface MvpViewSetTypePicker : BaseActivityMvpView {

    fun getSelectedExercisesCount(): Int
}