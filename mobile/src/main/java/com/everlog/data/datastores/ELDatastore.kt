package com.everlog.data.datastores

import com.everlog.data.datastores.exercises.ELExercisesStore
import com.everlog.data.datastores.exercises.ELUserExerciseStore
import com.everlog.data.datastores.history.ELUserWorkoutStore
import com.everlog.data.datastores.history.ELUserWorkoutsStore
import com.everlog.data.datastores.plans.ELUserPlanStore
import com.everlog.data.datastores.plans.ELUserPlansStore
import com.everlog.data.datastores.routines.ELUserRoutineStore
import com.everlog.data.datastores.routines.ELUserRoutinesStore

class ELDatastore {

    companion object {

        private var mWorkoutsStore: ELUserWorkoutsStore? = null
        private var mWorkoutStore: ELUserWorkoutStore? = null
        private var mRoutinesStore: ELUserRoutinesStore? = null
        private var mRoutineStore: ELUserRoutineStore? = null
        private var mExercisesStore: ELExercisesStore? = null
        private var mExerciseStore: ELUserExerciseStore? = null
        private var mUserStore: ELUserStore? = null
        private var mPlansStore: ELUserPlansStore? = null
        private var mPlanStore: ELUserPlanStore? = null
        private var mIntegrationStore: ELUserIntegrationStore? = null
        private var mConsentStore: ELUserConsentStore? = null
        private var mDeviceStore: ELUserDeviceStore? = null

        @JvmStatic
        @Synchronized
        fun workoutsStore(): ELUserWorkoutsStore {
            if (mWorkoutsStore == null) {
                mWorkoutsStore = ELUserWorkoutsStore()
            }
            return mWorkoutsStore!!
        }

        @JvmStatic
        @Synchronized
        fun workoutStore(): ELUserWorkoutStore {
            if (mWorkoutStore == null) {
                mWorkoutStore = ELUserWorkoutStore()
            }
            return mWorkoutStore!!
        }

        @JvmStatic
        @Synchronized
        fun routinesStore(): ELUserRoutinesStore {
            if (mRoutinesStore == null) {
                mRoutinesStore = ELUserRoutinesStore()
            }
            return mRoutinesStore!!
        }

        @JvmStatic
        @Synchronized
        fun routineStore(): ELUserRoutineStore {
            if (mRoutineStore == null) {
                mRoutineStore = ELUserRoutineStore()
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
        fun plansStore(): ELUserPlansStore {
            if (mPlansStore == null) {
                mPlansStore = ELUserPlansStore()
            }
            return mPlansStore!!
        }

        @JvmStatic
        @Synchronized
        fun planStore(): ELUserPlanStore {
            if (mPlanStore == null) {
                mPlanStore = ELUserPlanStore()
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