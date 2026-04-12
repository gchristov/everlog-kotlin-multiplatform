package com.everlog.managers.firebase

import android.net.Uri
import com.everlog.data.datastores.ELDatastore.Companion.exerciseStore
import com.everlog.data.migration.Migration
import com.everlog.data.migration.MultiPartUpdateExercise
import com.everlog.data.model.exercise.ELExercise
import com.everlog.managers.auth.LocalUserManager
import com.everlog.managers.preferences.PreferencesManager
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.gson.Gson
import timber.log.Timber
import java.util.*

object FirebaseStorageManager : PreferencesManager() {

    private const val TAG = "FirebaseStorageManager"

    private enum class PreferenceKeys {
        EXERCISE_UPLOAD_TASK_IDS
    }

    private val mStorage: FirebaseStorage = FirebaseStorage.getInstance()
    private val mStorageReference: StorageReference

    init {
        mStorageReference = mStorage.reference
    }
    
    fun resumePendingUploads() {
        Timber.tag(TAG).i("Checking for pending uploads")
        resumePendingExercises()
    }

    @JvmStatic
    fun uploadExerciseImage(filePath: Uri, exercise: ELExercise) {
        val ref = mStorageReference.child(getUserExercisePath(exercise))
        saveExerciseUpload(ref.toString(), exercise)
        resumeUpload(ref, filePath)
    }

    @JvmStatic
    fun deleteExerciseImage(exercise: ELExercise) {
        val ref = mStorageReference.child(getUserExercisePath(exercise))
        deleteImage(ref)
    }

    private fun deleteImage(ref: StorageReference) {
        ref.delete().addOnCompleteListener { task: Task<Void?> -> Timber.tag(TAG).d("Deleted: url=$ref, success=${task.isSuccessful}") }
    }

    private fun resumeUpload(ref: StorageReference, filePath: Uri) {
        Timber.tag(TAG).d("Resuming image upload: url=$ref, file=$filePath")
        val upload = ref.putFile(filePath)
        upload.addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot -> finishUpload(taskSnapshot.storage.toString()) }.addOnFailureListener { e: Exception ->
            e.printStackTrace()
            Timber.tag(TAG).e(e)
        }
        upload.resume()
    }

    private fun getUserExercisePath(exercise: ELExercise): String {
        return String.format("exercises/%s/%s", LocalUserManager.getUser()!!.id, exercise.uuid)
    }

    // Exercises

    private fun resumePendingExercises() {
        val exerciseUploadKeys = getPreference(PreferenceKeys.EXERCISE_UPLOAD_TASK_IDS.name, HashSet())
        for (uploadKey in exerciseUploadKeys) {
            try {
                val ref = mStorage.getReferenceFromUrl(uploadKey)
                val dataJson = getPreference(uploadKey, "")
                val (_, _, _, _, _, imageUrl) = Gson().fromJson(dataJson, ELExercise::class.java)
                resumeUpload(ref, Uri.parse(imageUrl))
            } catch (e: Exception) {
                e.printStackTrace()
                Timber.tag(TAG).e(e)
            }
        }
    }

    private fun saveExerciseUpload(uploadUrl: String?, exercise: ELExercise) {
        saveStorageUpload(uploadUrl, Gson().toJson(exercise), PreferenceKeys.EXERCISE_UPLOAD_TASK_IDS.name)
    }

    private fun finishExerciseUpload(exercise: ELExercise) {
        // Fetch the final image url.
        val ref = mStorageReference.child(getUserExercisePath(exercise))
        Timber.tag(TAG).d("Finished exercise image upload: url=$ref")
        ref.downloadUrl
                .addOnSuccessListener { uri: Uri ->
                    // Set the image url to the exercise and refresh Firestore.
                    exercise.imageUrl = uri.toString()
                    exerciseStore().create(exercise, SetOptions.merge())
                    Timber.tag(TAG).i("Updated exercise image in Firestore")
                    migrateRoutinesAndHistory(exercise)
                }
    }

    private fun migrateRoutinesAndHistory(changed: ELExercise) {
        val migration: Migration = MultiPartUpdateExercise(changed)
        migration.migrate(FirestorePathManager)
    }

    // Storage

    private fun saveStorageUpload(uploadUrl: String?,
                                  dataJson: String,
                                  uploadTypes: String) {
        if (uploadUrl != null) {
            // Save the data corresponding to this upload.
            savePreference(dataJson, uploadUrl)
            // Add the upload to the list of ongoing uploads of this type.
            val uploadKeys = getPreference(uploadTypes, HashSet())
            uploadKeys.add(uploadUrl)
            savePreference(uploadKeys, uploadTypes)
        }
    }

    private fun finishUpload(uploadUrl: String?) {
        Timber.tag(TAG).d("Finished image upload: url=$uploadUrl")
        if (uploadUrl != null) {
            val dataJson = getPreference(uploadUrl, "")
            val exerciseUploadKeys = getPreference(PreferenceKeys.EXERCISE_UPLOAD_TASK_IDS.name, HashSet())
            if (exerciseUploadKeys.contains(uploadUrl)) {
                // An exercise image has finished.
                exerciseUploadKeys.remove(uploadUrl)
                savePreference(exerciseUploadKeys, PreferenceKeys.EXERCISE_UPLOAD_TASK_IDS.name)
                val exercise = Gson().fromJson(dataJson, ELExercise::class.java)
                finishExerciseUpload(exercise)
            }
            removePreference(uploadUrl)
        }
    }
}