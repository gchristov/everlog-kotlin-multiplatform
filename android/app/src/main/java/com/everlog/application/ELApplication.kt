package com.everlog.application

import android.app.Application
import com.everlog.android.common.data.CommonData
import com.everlog.android.common.kotlin.AppContext
import com.everlog.android.common.mvp.CommonMvp
import com.everlog.android.exercise.feature.ExerciseFeature
import com.everlog.application.features.AppCommonDataDependencies
import com.everlog.application.features.AppCommonMvpDependencies
import com.everlog.application.features.AppExerciseFeatureDependencies
import com.everlog.config.AppConfig
import com.jakewharton.threetenabp.AndroidThreeTen

class ELApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext = this
        CommonData.dependencies = AppCommonDataDependencies()
        CommonMvp.dependencies = AppCommonMvpDependencies()
        ExerciseFeature.dependencies = AppExerciseFeatureDependencies()
        AndroidThreeTen.init(this)
        AppConfig.configuration.configureApp()
    }
}
