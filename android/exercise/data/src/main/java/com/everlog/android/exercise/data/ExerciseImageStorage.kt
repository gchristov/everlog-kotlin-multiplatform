package com.everlog.android.exercise.data

import android.net.Uri
import com.everlog.android.common.data.model.exercise.ELExercise

// Uploads the user's exercise images, resuming the ones a closed app left unfinished
interface ExerciseImageStorage {
    fun resumePendingUploads()

    fun uploadExerciseImage(filePath: Uri, exercise: ELExercise, userId: String)

    fun deleteExerciseImage(exercise: ELExercise, userId: String)
}
