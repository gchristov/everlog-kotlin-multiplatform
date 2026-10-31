package com.everlog.data.datastores

import com.everlog.android.auth.data.store.AuthStores
import com.everlog.android.auth.data.store.ELUserConsentStore
import com.everlog.android.auth.data.store.ELUserDeviceStore
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.store.PlanStores
import com.everlog.android.settings.data.store.ELUserIntegrationStore
import com.everlog.android.settings.data.store.SettingsStores
import com.everlog.android.common.data.store.CollectionStore
import com.everlog.android.common.data.store.DocumentStore
import com.everlog.android.exercise.data.store.ELExercisesStore
import com.everlog.android.exercise.data.store.ELUserExerciseStore
import com.everlog.android.exercise.data.store.ExerciseStores
import com.everlog.android.template.data.store.ELUserRoutineInMemoryStore
import com.everlog.android.template.data.store.ELUserRoutineStore
import com.everlog.android.template.data.store.ELUserRoutinesInMemoryStore
import com.everlog.android.template.data.store.ELUserRoutinesStore
import com.everlog.android.template.data.store.TemplateStores
import com.everlog.android.workout.data.store.WorkoutStores
import com.everlog.data.datastores.ELUserStore

class ELDatastore {

    companion object {

        private var mUserStore: ELUserStore? = null

        @JvmStatic
        fun workoutsStore(): CollectionStore<ELWorkout> = WorkoutStores.workoutsStore()

        @JvmStatic
        fun workoutStore(): DocumentStore<ELWorkout> = WorkoutStores.workoutStore()

        @JvmStatic
        fun routinesStore(): CollectionStore<ELRoutine> = TemplateStores.routinesStore()

        @JvmStatic
        fun routineStore(): DocumentStore<ELRoutine> = TemplateStores.routineStore()

        @JvmStatic
        fun exercisesStore(): ELExercisesStore = ExerciseStores.exercisesStore()

        @JvmStatic
        fun exerciseStore(): ELUserExerciseStore = ExerciseStores.exerciseStore()

        @JvmStatic
        @Synchronized
        fun userStore(): ELUserStore {
            if (mUserStore == null) {
                mUserStore = ELUserStore()
            }
            return mUserStore!!
        }

        @JvmStatic
        fun plansStore(): CollectionStore<ELPlan> = PlanStores.plansStore()

        @JvmStatic
        fun planStore(): DocumentStore<ELPlan> = PlanStores.planStore()

        @JvmStatic
        fun integrationStore(): ELUserIntegrationStore = SettingsStores.integrationStore()

        @JvmStatic
        fun consentStore(): ELUserConsentStore = AuthStores.consentStore()

        @JvmStatic
        fun deviceStore(): ELUserDeviceStore = AuthStores.deviceStore()

        @JvmStatic
        fun destroy() {
            WorkoutStores.destroy()
            TemplateStores.destroy()
            ExerciseStores.destroy()
            mUserStore?.destroy()
            mUserStore = null
            PlanStores.destroy()
            SettingsStores.destroy()
            AuthStores.destroy()
        }
    }
}