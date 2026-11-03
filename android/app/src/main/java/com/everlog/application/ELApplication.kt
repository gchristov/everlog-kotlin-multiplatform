package com.everlog.application

import android.app.Application
import com.everlog.android.auth.data.store.AuthStores
import com.everlog.android.common.data.CommonData
import com.everlog.android.common.kotlin.AppContext
import com.everlog.android.common.kotlin.di.GraphOwner
import com.everlog.android.common.mvp.CommonMvp
import com.everlog.android.exercise.data.store.ExerciseStores
import com.everlog.android.plan.data.store.PlanStores
import com.everlog.android.settings.data.store.SettingsStores
import com.everlog.android.template.data.store.TemplateStores
import com.everlog.android.workout.data.store.WorkoutStores
import com.everlog.application.features.AppCommonDataDependencies
import com.everlog.application.features.AppCommonMvpDependencies
import com.everlog.config.AppConfig
import com.jakewharton.threetenabp.AndroidThreeTen
import dev.zacsweers.metro.createGraphFactory

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
        graph = createGraphFactory<AppGraph.Factory>().create(this)
        CommonData.dependencies = AppCommonDataDependencies()
        AuthStores.session = graph.userSession
        ExerciseStores.session = graph.userSession
        TemplateStores.session = graph.userSession
        WorkoutStores.session = graph.userSession
        PlanStores.session = graph.userSession
        SettingsStores.session = graph.userSession
        CommonMvp.dependencies = AppCommonMvpDependencies()
        AndroidThreeTen.init(this)
        AppConfig.configuration.configureApp()
    }
}
