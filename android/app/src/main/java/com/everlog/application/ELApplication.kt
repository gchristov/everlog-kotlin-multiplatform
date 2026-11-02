package com.everlog.application

import android.app.Application
import com.everlog.android.auth.AuthFeature
import com.everlog.android.auth.data.store.AuthStores
import com.everlog.android.common.data.CommonData
import com.everlog.android.common.kotlin.AppContext
import com.everlog.android.common.kotlin.di.GraphOwner
import com.everlog.android.common.mvp.CommonMvp
import com.everlog.android.exercise.data.store.ExerciseStores
import com.everlog.android.history.HistoryFeature
import com.everlog.android.onboarding.OnboardingFeature
import com.everlog.android.plan.PlanFeature
import com.everlog.android.plan.data.store.PlanStores
import com.everlog.android.pro.ProFeature
import com.everlog.android.settings.SettingsFeature
import com.everlog.android.settings.data.store.SettingsStores
import com.everlog.android.template.TemplateFeature
import com.everlog.android.template.data.store.TemplateStores
import com.everlog.android.workout.WorkoutFeature
import com.everlog.android.workout.data.store.WorkoutStores
import com.everlog.application.features.AppAuthFeatureDependencies
import com.everlog.application.features.AppCommonDataDependencies
import com.everlog.application.features.AppCommonMvpDependencies
import com.everlog.application.features.AppHistoryFeatureDependencies
import com.everlog.application.features.AppOnboardingFeatureDependencies
import com.everlog.application.features.AppPlanFeatureDependencies
import com.everlog.application.features.AppProFeatureDependencies
import com.everlog.application.features.AppSettingsFeatureDependencies
import com.everlog.application.features.AppTemplateFeatureDependencies
import com.everlog.application.features.AppWorkoutFeatureDependencies
import com.everlog.config.AppConfig
import com.jakewharton.threetenabp.AndroidThreeTen
import dev.zacsweers.metro.createGraph

class ELApplication : Application(), GraphOwner {

    companion object {
        // For the code that can't have its dependencies passed in yet
        @JvmStatic
        lateinit var graph: AppGraph
            private set
    }

    override val dependencyGraph: Any
        get() = graph

    override fun onCreate() {
        super.onCreate()
        AppContext = this
        graph = createGraph<AppGraph>()
        CommonData.dependencies = AppCommonDataDependencies()
        AuthStores.session = graph.userSession
        ExerciseStores.session = graph.userSession
        TemplateStores.session = graph.userSession
        WorkoutStores.session = graph.userSession
        PlanStores.session = graph.userSession
        SettingsStores.session = graph.userSession
        CommonMvp.dependencies = AppCommonMvpDependencies()
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
