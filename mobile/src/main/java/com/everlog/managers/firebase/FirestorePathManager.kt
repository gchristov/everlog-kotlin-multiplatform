package com.everlog.managers.firebase

import com.everlog.BuildConfig
import com.everlog.data.model.ELUser
import com.everlog.managers.auth.LocalUserManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

object FirestorePathManager {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        FirebaseFirestore.setLoggingEnabled(BuildConfig.DEBUG)
    }

    val globalExercisesCollection: CollectionReference
        get() = getCollectionReference("global/exercises/all")

    val usersCollection: CollectionReference
        get() = getCollectionReference("users")

    val exercisesCollection: CollectionReference
        get() {
            val user = LocalUserManager.getUser()
                    ?: throw IllegalStateException("User missing or invalid")
            return getCollectionReference(buildUserSpecificPath(user, "exercises"))
        }

    val devicesCollection: CollectionReference
        get() {
            val user = LocalUserManager.getUser()
                    ?: throw IllegalStateException("User missing or invalid")
            return getCollectionReference(buildUserSpecificPath(user, "devices"))
        }

    val routinesCollection: CollectionReference
        get() {
            val user = LocalUserManager.getUser()
                    ?: throw IllegalStateException("User missing or invalid")
            return getCollectionReference(buildUserSpecificPath(user, "routines"))
        }

    val workoutsCollection: CollectionReference
        get() {
            val user = LocalUserManager.getUser()
                    ?: throw IllegalStateException("User missing or invalid")
            return getCollectionReference(buildUserSpecificPath(user, "history"))
        }

    val plansCollection: CollectionReference
        get() = getCollectionReference(buildPlansPath())

    val integrationsCollection: CollectionReference
        get() = getCollectionReference(buildIntegrationsPath())

    val consentCollection: CollectionReference
        get() = getCollectionReference(buildConsentPath())

    // Utils

    private fun getCollectionReference(path: String?): CollectionReference {
        if (path != null) {
            return firestore.collection(path)
        }
        throw IllegalStateException("Collection path missing or invalid")
    }

    private fun buildPlansPath(): String {
        return "plans"
    }

    private fun buildIntegrationsPath(): String {
        return "integrations"
    }

    private fun buildConsentPath(): String {
        return "consent"
    }

    private fun buildUserSpecificPath(user: ELUser, path: String): String {
        return String.format("users/%s/%s", user.id, path)
    }
}