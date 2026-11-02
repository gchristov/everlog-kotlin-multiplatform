package com.everlog.android.exercise.data

import android.net.Uri
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.persistence.PreferencesManager
import com.everlog.android.exercise.data.store.ExerciseStores
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.gson.Gson
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.util.*
import timber.log.Timber

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ExerciseImageStorage>())
@Inject
class RealExerciseImageStorage(private val copies: ExerciseCopies) : PreferencesManager(), ExerciseImageStorage {

    private companion object {
        const val TAG = "ExerciseImageStorage"
    }

    private enum class PreferenceKeys {
        EXERCISE_UPLOAD_TASK_IDS
    }

    private val mStorage: FirebaseStorage = FirebaseStorage.getInstance()
    private val mStorageReference: StorageReference

    init {
        mStorageReference = mStorage.reference
    }
    
    override fun resumePendingUploads() {
        Timber.tag(TAG).i("Checking for pending uploads")
        resumePendingExercises()
    }

    override fun uploadExerciseImage(filePath: Uri, exercise: ELExercise, userId: String) {
        val ref = mStorageReference.child(getUserExercisePath(exercise, userId))
        saveExerciseUpload(ref.toString(), exercise)
        resumeUpload(ref, filePath)
    }

    override fun deleteExerciseImage(exercise: ELExercise, userId: String) {
        val ref = mStorageReference.child(getUserExercisePath(exercise, userId))
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

    private fun getUserExercisePath(exercise: ELExercise, userId: String): String {
        return String.format("exercises/%s/%s", userId, exercise.uuid)
    }

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

    private fun finishExerciseUpload(ref: StorageReference, exercise: ELExercise) {
        // Fetch the final image url.
        Timber.tag(TAG).d("Finished exercise image upload: url=$ref")
        ref.downloadUrl
                .addOnSuccessListener { uri: Uri ->
                    // Set the image url to the exercise and refresh Firestore.
                    exercise.imageUrl = uri.toString()
                    ExerciseStores.exerciseStore().create(exercise, SetOptions.merge())
                    Timber.tag(TAG).i("Updated exercise image in Firestore")
                    copies.update(exercise)
                }
    }


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
                finishExerciseUpload(mStorage.getReferenceFromUrl(uploadUrl), exercise)
            }
            removePreference(uploadUrl)
        }
    }
}