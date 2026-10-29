package com.everlog.data.model.exercise

import com.everlog.constants.ELConstants
import com.everlog.data.model.ELFirestoreModel
import com.everlog.managers.auth.LocalUserManager
import java.io.Serializable
import java.util.*

data class ELExercise (

        var uuid: String? = null,
        var name: String? = null,
        var category: String? = null,
        var splitSides: Boolean = false,
        var createdDate: Long = 0,
        var imageUrl: String? = null,
        var createdByUserId: String? = null,
        var privateToUser: Boolean = false

) : Serializable, ELFirestoreModel {

    companion object {

        @JvmStatic
        fun newExercise(userId: String): ELExercise {
            val exercise = ELExercise()
            exercise.setCreatedDateAsDate(Date())
            exercise.uuid = UUID.randomUUID().toString()
            exercise.createdByUserId = userId
            exercise.privateToUser = true
            return exercise
        }

        fun newExercise(userId: String,
                        suggestion: ExerciseSuggestion,
                        category: String): ELExercise {
            val exercise = ELExercise()
            exercise.setCreatedDateAsDate(Date())
            exercise.uuid = UUID.randomUUID().toString()
            exercise.createdByUserId = userId
            exercise.privateToUser = true
            exercise.name = suggestion.name
            exercise.category = category
            return exercise
        }
    }

    override fun documentId(): String {
        return uuid!!
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map[ELConstants.FIELD_UUID] = uuid
        map["splitSides"] = splitSides
        map["category"] = category
        map["imageUrl"] = imageUrl
        map[ELConstants.FIELD_CREATED_BY_USER_ID] = createdByUserId
        map["privateToUser"] = privateToUser
        map[ELConstants.FIELD_NAME] = name
        map[ELConstants.FIELD_CREATED_DATE] = createdDate
        return map
    }

    fun getCreatedDateAsDate(): Date? {
        return Date(createdDate)
    }

    private fun setCreatedDateAsDate(date: Date) {
        createdDate = date.time
    }

    fun getFirstChar(): String {
        return name?.get(0).toString()
    }

    fun isEditable(): Boolean {
        return createdByUserId == LocalUserManager.getUser()?.id
    }

    fun isLowerBody(): Boolean {
        return "LEGS".equals(category, ignoreCase = true)
    }
}