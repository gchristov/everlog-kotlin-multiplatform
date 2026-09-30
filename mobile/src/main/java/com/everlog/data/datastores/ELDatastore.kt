package com.everlog.data.datastores

import com.everlog.data.datastores.base.CollectionStore
import com.everlog.data.datastores.base.DocumentStore
import com.everlog.data.datastores.base.InMemoryCollectionStore
import com.everlog.data.datastores.base.InMemoryDatabase
import com.everlog.data.datastores.base.InMemoryDocumentStore
import com.everlog.data.datastores.exercises.ELExercisesStore
import com.everlog.data.datastores.exercises.ELUserExerciseStore
import com.everlog.data.datastores.history.ELUserWorkoutStore
import com.everlog.data.datastores.history.ELUserWorkoutStore.ELDocStoreWorkoutLoadedEvent
import com.everlog.data.datastores.history.ELUserWorkoutsStore
import com.everlog.data.datastores.history.ELUserWorkoutsStore.ELColStoreWorkoutsLoadedEvent
import com.everlog.data.datastores.plans.ELUserPlanStore
import com.everlog.data.datastores.plans.ELUserPlanStore.ELDocStorePlanLoadedEvent
import com.everlog.data.datastores.plans.ELUserPlansStore
import com.everlog.data.datastores.plans.ELUserPlansStore.ELColStorePlansLoadedEvent
import com.everlog.data.datastores.routines.ELRoutineDecorator
import com.everlog.data.datastores.routines.ELUserRoutineStore
import com.everlog.data.datastores.routines.ELUserRoutineStore.ELDocStoreRoutineLoadedEvent
import com.everlog.data.datastores.routines.ELUserRoutinesStore
import com.everlog.data.datastores.routines.ELUserRoutinesStore.ELColStoreRoutinesLoadedEvent
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.plan.ELPlan
import com.everlog.data.model.workout.ELWorkout
import com.everlog.utils.device.DeviceUtils

class ELDatastore {

    companion object {

        // Firebase Test Lab runs keep these in memory, so robots don't create data
        private const val ROUTINES = "routines"
        private const val PLANS = "plans"
        private const val WORKOUTS = "workouts"

        private var mWorkoutsStore: CollectionStore<ELWorkout>? = null
        private var mWorkoutStore: DocumentStore<ELWorkout>? = null
        private var mRoutinesStore: CollectionStore<ELRoutine>? = null
        private var mRoutineStore: DocumentStore<ELRoutine>? = null
        private var mExercisesStore: ELExercisesStore? = null
        private var mExerciseStore: ELUserExerciseStore? = null
        private var mUserStore: ELUserStore? = null
        private var mPlansStore: CollectionStore<ELPlan>? = null
        private var mPlanStore: DocumentStore<ELPlan>? = null
        private var mIntegrationStore: ELUserIntegrationStore? = null
        private var mConsentStore: ELUserConsentStore? = null
        private var mDeviceStore: ELUserDeviceStore? = null

        @JvmStatic
        @Synchronized
        fun workoutsStore(): CollectionStore<ELWorkout> {
            if (mWorkoutsStore == null) {
                mWorkoutsStore = if (inMemory()) {
                    InMemoryCollectionStore<ELWorkout>(WORKOUTS, ::ELColStoreWorkoutsLoadedEvent, compareByDescending { it.createdDate }, ::decorateWorkout)
                } else {
                    ELUserWorkoutsStore()
                }
            }
            return mWorkoutsStore!!
        }

        @JvmStatic
        @Synchronized
        fun workoutStore(): DocumentStore<ELWorkout> {
            if (mWorkoutStore == null) {
                mWorkoutStore = if (inMemory()) {
                    InMemoryDocumentStore(WORKOUTS, ELWorkout::class.java, ::ELDocStoreWorkoutLoadedEvent, ::decorateWorkout)
                } else {
                    ELUserWorkoutStore()
                }
            }
            return mWorkoutStore!!
        }

        @JvmStatic
        @Synchronized
        fun routinesStore(): CollectionStore<ELRoutine> {
            if (mRoutinesStore == null) {
                mRoutinesStore = if (inMemory()) {
                    InMemoryCollectionStore<ELRoutine>(ROUTINES, ::ELColStoreRoutinesLoadedEvent, compareBy(nullsFirst()) { it.name }, ::decorateRoutine)
                } else {
                    ELUserRoutinesStore()
                }
            }
            return mRoutinesStore!!
        }

        @JvmStatic
        @Synchronized
        fun routineStore(): DocumentStore<ELRoutine> {
            if (mRoutineStore == null) {
                mRoutineStore = if (inMemory()) {
                    InMemoryDocumentStore(ROUTINES, ELRoutine::class.java, ::ELDocStoreRoutineLoadedEvent, ::decorateRoutine)
                } else {
                    ELUserRoutineStore()
                }
            }
            return mRoutineStore!!
        }

        @JvmStatic
        @Synchronized
        fun exercisesStore(): ELExercisesStore {
            if (mExercisesStore == null) {
                mExercisesStore = ELExercisesStore()
            }
            return mExercisesStore!!
        }

        @JvmStatic
        @Synchronized
        fun exerciseStore(): ELUserExerciseStore {
            if (mExerciseStore == null) {
                mExerciseStore = ELUserExerciseStore()
            }
            return mExerciseStore!!
        }

        @JvmStatic
        @Synchronized
        fun userStore(): ELUserStore {
            if (mUserStore == null) {
                mUserStore = ELUserStore()
            }
            return mUserStore!!
        }

        @JvmStatic
        @Synchronized
        fun plansStore(): CollectionStore<ELPlan> {
            if (mPlansStore == null) {
                mPlansStore = if (inMemory()) {
                    InMemoryCollectionStore<ELPlan>(PLANS, ::ELColStorePlansLoadedEvent, compareBy(nullsFirst()) { it.name }, ::decoratePlan)
                } else {
                    ELUserPlansStore()
                }
            }
            return mPlansStore!!
        }

        @JvmStatic
        @Synchronized
        fun planStore(): DocumentStore<ELPlan> {
            if (mPlanStore == null) {
                mPlanStore = if (inMemory()) {
                    InMemoryDocumentStore(PLANS, ELPlan::class.java, ::ELDocStorePlanLoadedEvent, ::decoratePlan)
                } else {
                    ELUserPlanStore()
                }
            }
            return mPlanStore!!
        }

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

        private fun inMemory(): Boolean {
            return DeviceUtils.isFirebaseTestLabRun()
        }

        // The in-memory stores decorate items the same way as the Firestore ones

        private fun decorateRoutine(routine: ELRoutine) {
            ELRoutineDecorator().decorate(routine)
        }

        private fun decorateWorkout(workout: ELWorkout) {
            ELRoutineDecorator().decorate(workout)
        }

        // ELPlanDecorator looks routines up in Firestore, so resolve them from memory instead
        private fun decoratePlan(plan: ELPlan) {
            val routineDecorator = ELRoutineDecorator()
            val routines = plan.getRoutinesToResolve().mapNotNull { uuid ->
                (InMemoryDatabase.get(ROUTINES, uuid) as? ELRoutine)?.let { routine ->
                    routineDecorator.decorate(routine)
                    uuid to routine
                }
            }.toMap()
            if (routines.isNotEmpty()) {
                plan.resolveRoutines(routines)
            }
        }

        @JvmStatic
        fun destroy() {
            mWorkoutsStore?.destroy()
            mWorkoutsStore = null
            mWorkoutStore?.destroy()
            mWorkoutStore = null
            mRoutinesStore?.destroy()
            mRoutinesStore = null
            mRoutineStore?.destroy()
            mRoutineStore = null
            mExercisesStore?.destroy()
            mExercisesStore = null
            mExerciseStore?.destroy()
            mExerciseStore = null
            mUserStore?.destroy()
            mUserStore = null
            mPlansStore?.destroy()
            mPlansStore = null
            mPlanStore?.destroy()
            mPlanStore = null
            mIntegrationStore?.destroy()
            mIntegrationStore = null
            mConsentStore?.destroy()
            mConsentStore = null
            mDeviceStore?.destroy()
            mDeviceStore = null
        }
    }
}