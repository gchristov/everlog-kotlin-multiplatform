package com.everlog.data.datastores

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.store.PlanStores
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

class ELDatastore {

    companion object {

        private var mUserStore: ELUserStore? = null
        private var mIntegrationStore: ELUserIntegrationStore? = null
        private var mConsentStore: ELUserConsentStore? = null
        private var mDeviceStore: ELUserDeviceStore? = null

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
        @Synchronized
        fun integrationStore(): ELUserIntegrationStore {
            if (mIntegrationStore == null) {
                mIntegrationStore = ELUserIntegrationStore()
            }
            return mIntegrationStore!!
        }

        @JvmStatic
        @Synchronized
        fun consentStore(): ELUserConsentStore {
            if (mConsentStore == null) {
                mConsentStore = ELUserConsentStore()
            }
            return mConsentStore!!
        }

        @JvmStatic
        @Synchronized
        fun deviceStore(): ELUserDeviceStore {
            if (mDeviceStore == null) {
                mDeviceStore = ELUserDeviceStore()
            }
            return mDeviceStore!!
        }

        @JvmStatic
        fun destroy() {
            WorkoutStores.destroy()
            TemplateStores.destroy()
            ExerciseStores.destroy()
            mUserStore?.destroy()
            mUserStore = null
            PlanStores.destroy()
            mIntegrationStore?.destroy()
            mIntegrationStore = null
            mConsentStore?.destroy()
            mConsentStore = null
            mDeviceStore?.destroy()
            mDeviceStore = null
        }
    }
}