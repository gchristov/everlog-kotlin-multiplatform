package com.everlog.managers.firebase

import com.everlog.android.auth.data.LocalUserManager
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
        get() = FirestorePaths.exercises(LocalUserManager.requireUserId())

    val devicesCollection: CollectionReference
        get() = FirestorePaths.devices(LocalUserManager.requireUserId())

    val routinesCollection: CollectionReference
        get() = FirestorePaths.routines(LocalUserManager.requireUserId())

    val workoutsCollection: CollectionReference
        get() = FirestorePaths.workouts(LocalUserManager.requireUserId())

    val plansCollection: CollectionReference
        get() = FirestorePaths.plans

    val integrationsCollection: CollectionReference
        get() = FirestorePaths.integrations

    val consentCollection: CollectionReference
        get() = FirestorePaths.consent
}
