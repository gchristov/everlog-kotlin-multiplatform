package com.everlog.android.common.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

object FirestorePaths {

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    val globalExercises: CollectionReference
        get() = firestore.collection("global/exercises/all")

    // The ids the exercise library uses, with their names (tools/exercises/README.md)
    val exerciseRegistry: DocumentReference
        get() = firestore.document("global/exercises/registry/current")

    val users: CollectionReference
        get() = firestore.collection("users")

    val plans: CollectionReference
        get() = firestore.collection("plans")

    val integrations: CollectionReference
        get() = firestore.collection("integrations")

    val consent: CollectionReference
        get() = firestore.collection("consent")

    fun exercises(userId: String): CollectionReference = userCollection(userId, "exercises")

    fun devices(userId: String): CollectionReference = userCollection(userId, "devices")

    fun routines(userId: String): CollectionReference = userCollection(userId, "routines")

    fun workouts(userId: String): CollectionReference = userCollection(userId, "history")

    private fun userCollection(userId: String, name: String): CollectionReference =
        firestore.collection("users/$userId/$name")
}
