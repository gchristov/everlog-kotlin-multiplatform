package com.everlog.managers.firebase

import com.everlog.android.auth.data.requireUserId
import com.everlog.application.AppServices
import com.everlog.android.common.firebase.FirestorePaths
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference

// The paths for the signed-in user, for code that reads the user from the app's session
object FirestorePathManager {

    val globalExercisesCollection: CollectionReference
        get() = FirestorePaths.globalExercises

    val exerciseRegistryDocument: DocumentReference
        get() = FirestorePaths.exerciseRegistry

    val usersCollection: CollectionReference
        get() = FirestorePaths.users

    val exercisesCollection: CollectionReference
        get() = FirestorePaths.exercises(AppServices.userSession.requireUserId())

    val devicesCollection: CollectionReference
        get() = FirestorePaths.devices(AppServices.userSession.requireUserId())

    val routinesCollection: CollectionReference
        get() = FirestorePaths.routines(AppServices.userSession.requireUserId())

    val workoutsCollection: CollectionReference
        get() = FirestorePaths.workouts(AppServices.userSession.requireUserId())

    val plansCollection: CollectionReference
        get() = FirestorePaths.plans

    val integrationsCollection: CollectionReference
        get() = FirestorePaths.integrations

    val consentCollection: CollectionReference
        get() = FirestorePaths.consent
}
