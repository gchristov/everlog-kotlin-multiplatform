package com.everlog.data.migration;

import com.everlog.data.datastores.ELDatastore;
import com.everlog.data.datastores.base.OnStoreItemsListener;
import com.everlog.data.model.ELRoutine;
import com.everlog.data.model.exercise.ELExercise;
import com.everlog.data.model.workout.ELWorkout;
import com.everlog.managers.firebase.FirestorePathManager;
import com.google.firebase.firestore.SetOptions;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import timber.log.Timber;

public class MultiPartUpdateExercise implements Migration {

    private static final String TAG = "MultiPartUpdateExercise";

    private static MigrationThread mPreviousMigration;

    private ELExercise changedExercise;

    public MultiPartUpdateExercise(ELExercise changedExercise) {
        this.changedExercise = changedExercise;
    }

    @Override
    public void migrate(@NotNull FirestorePathManager pathManager) {
        if (mPreviousMigration != null) {
            mPreviousMigration.cancel();
        }
        mPreviousMigration = new MigrationThread(changedExercise);
        mPreviousMigration.start();
    }

    private static class MigrationThread extends Thread {

        private ELExercise mChangedExercise;
        private boolean mIsCancelled;

        MigrationThread(ELExercise changedExercise) {
            this.mChangedExercise = changedExercise;
        }

        public void cancel() {
            mIsCancelled = true;
        }

        @Override
        public void run() {
            Timber.tag(TAG).i("Starting migration in background");
            if (mChangedExercise != null) {
                migrateRoutines(mChangedExercise);
                migrateHistory(mChangedExercise);
            } else {
                Timber.tag(TAG).e("Missing user or exercise");
            }
        }

        private void migrateRoutines(ELExercise changedExercise) {
            Timber.tag(TAG).i("Running routines migration");
            ELDatastore.routinesStore().getItems(new OnStoreItemsListener<ELRoutine>() {

                @Override
                public void onItemsLoaded(List<ELRoutine> items, boolean fromCache) {
                    for (ELRoutine routine : items) {
                        if (mIsCancelled) {
                            Timber.tag(TAG).i("Cancelled");
                            return;
                        }
                        if (routine.updateExercise(changedExercise)) {
                            Timber.tag(TAG).d("Found matching routine, saving");
                            ELDatastore.routineStore().create(routine, SetOptions.merge());
                        }
                    }
                    Timber.tag(TAG).i("Finished routines migration");
                }

                @Override
                public void onItemsLoadingError(Throwable throwable) {
                    Timber.tag(TAG).e("Failed to fetch routines: error=%s", throwable.getMessage());
                }
            });
        }

        private void migrateHistory(ELExercise changedExercise) {
            Timber.tag(TAG).i("Running history migration");
            ELDatastore.workoutsStore().getItems(new OnStoreItemsListener<ELWorkout>() {

                @Override
                public void onItemsLoaded(List<ELWorkout> items, boolean fromCache) {
                    for (ELWorkout workout : items) {
                        if (mIsCancelled) {
                            Timber.tag(TAG).i("Cancelled");
                            return;
                        }
                        if (workout.updateExercise(changedExercise)) {
                            Timber.tag(TAG).d("Found matching history item, saving");
                            ELDatastore.workoutStore().create(workout, SetOptions.merge());
                        }
                    }
                    Timber.tag(TAG).i("Finished history migration");
                }

                @Override
                public void onItemsLoadingError(Throwable throwable) {
                    Timber.tag(TAG).e("Failed to fetch history: error=%s", throwable.getMessage());
                }
            });
        }
    }
}
