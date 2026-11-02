package com.everlog.android.common.kotlin.di

import android.content.Context

// The application, which holds the dependency graph for what the system creates: activities, fragments, services
interface GraphOwner {
    val dependencyGraph: Any
}

// The part of the app's graph a feature declares for its screens, e.g. appGraph<ExerciseGraph>()
inline fun <reified T : Any> Context.appGraph(): T = (applicationContext as GraphOwner).dependencyGraph as T

// The same for Java, which can't call the reified one
fun <T : Any> Context.appGraph(type: Class<T>): T = type.cast((applicationContext as GraphOwner).dependencyGraph)!!
