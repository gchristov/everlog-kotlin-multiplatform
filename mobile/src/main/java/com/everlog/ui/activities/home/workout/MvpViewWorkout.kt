package com.everlog.ui.activities.home.workout

import com.everlog.data.model.workout.ELWorkout
import com.everlog.data.model.workout.ELWorkoutState
import com.everlog.databinding.ActivityWorkoutBinding
import com.everlog.ui.activities.home.exercisegroup.MvpViewCreateExerciseGroups
import rx.Observable

interface MvpViewWorkout : MvpViewCreateExerciseGroups {

    fun getBinding(): ActivityWorkoutBinding

    fun onClickMuscleGoalStatus(): Observable<Void>

    fun onClickEmptyAction(): Observable<Void>

    fun getWorkout(): ELWorkout?

    fun performingFromPlan(): Boolean

    fun loadWorkoutDetails(workout: ELWorkout, hasExercises: Boolean)

    // Workout service

    fun onClickServiceIncreaseWeight(): Observable<ELWorkoutState>

    fun onClickServiceDecreaseWeight(): Observable<ELWorkoutState>

    fun onClickServiceIncreaseReps(): Observable<ELWorkoutState>

    fun onClickServiceDecreaseReps(): Observable<ELWorkoutState>

    fun onClickServiceTimerExerciseStart(): Observable<ELWorkoutState>

    fun onClickServiceTimerExerciseStop(): Observable<ELWorkoutState>

    fun onClickServiceTimerRestStop(): Observable<Void>

    fun onClickServiceCompleteSet(): Observable<ELWorkoutState>
}