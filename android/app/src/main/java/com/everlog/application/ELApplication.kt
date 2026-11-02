package com.everlog.application

import com.everlog.android.settings.data.store.SettingsStores
import com.everlog.android.plan.data.store.PlanStores
import com.everlog.android.workout.data.store.WorkoutStores
import com.everlog.android.template.data.store.TemplateStores
import com.everlog.android.exercise.data.store.ExerciseStores
import com.everlog.android.auth.data.store.AuthStores
import android.app.Application
import com.everlog.android.auth.AuthFeature
import com.everlog.android.common.data.CommonData
import com.everlog.android.common.kotlin.AppContext
import com.everlog.android.common.mvp.CommonMvp
import com.everlog.android.exercise.data.ExerciseData
import com.everlog.android.history.HistoryFeature
import com.everlog.android.onboarding.OnboardingFeature
import com.everlog.android.plan.PlanFeature
import com.everlog.android.pro.ProFeature
import com.everlog.android.settings.SettingsFeature
import com.everlog.android.exercise.ExerciseFeature
import com.everlog.android.template.TemplateFeature
import com.everlog.android.workout.WorkoutFeature
import com.everlog.application.features.AppAuthFeatureDependencies
import com.everlog.application.features.AppCommonDataDependencies
import com.everlog.application.features.AppCommonMvpDependencies
import com.everlog.application.features.AppExerciseDataDependencies
import com.everlog.application.features.AppExerciseFeatureDependencies
import com.everlog.application.features.AppHistoryFeatureDependencies
import com.everlog.application.features.AppOnboardingFeatureDependencies
import com.everlog.application.features.AppPlanFeatureDependencies
import com.everlog.application.features.AppProFeatureDependencies
import com.everlog.application.features.AppSettingsFeatureDependencies
import com.everlog.application.features.AppTemplateFeatureDependencies
import com.everlog.application.features.AppWorkoutFeatureDependencies
import com.everlog.config.AppConfig
import com.jakewharton.threetenabp.AndroidThreeTen

class ELApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext = this
        CommonData.dependencies = AppCommonDataDependencies()
        AuthStores.session = AppServices.userSession
        ExerciseStores.session = AppServices.userSession
        TemplateStores.session = AppServices.userSession
        WorkoutStores.session = AppServices.userSession
        PlanStores.session = AppServices.userSession
        SettingsStores.session = AppServices.userSession
        CommonMvp.dependencies = AppCommonMvpDependencies()
        ExerciseData.dependencies = AppExerciseDataDependencies()
        ExerciseFeature.dependencies = AppExerciseFeatureDependencies()
        TemplateFeature.dependencies = AppTemplateFeatureDependencies()
        WorkoutFeature.dependencies = AppWorkoutFeatureDependencies()
        HistoryFeature.dependencies = AppHistoryFeatureDependencies()
        PlanFeature.dependencies = AppPlanFeatureDependencies()
        SettingsFeature.dependencies = AppSettingsFeatureDependencies()
        ProFeature.dependencies = AppProFeatureDependencies()
        AuthFeature.dependencies = AppAuthFeatureDependencies()
        OnboardingFeature.dependencies = AppOnboardingFeatureDependencies()
        AndroidThreeTen.init(this)
        AppConfig.configuration.configureApp()
    }
}
