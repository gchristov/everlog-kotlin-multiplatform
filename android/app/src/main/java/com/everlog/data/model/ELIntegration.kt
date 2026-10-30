package com.everlog.data.model

import com.everlog.android.common.data.model.ELFirestoreModel
import com.everlog.android.common.firebase.FirestoreFields
import java.io.Serializable
import java.util.*

data class ELIntegration (

        var uuid: String? = null,
        var type: String? = null,
        var serverAuthCode: String? = null,
        var createdDate: Long = 0,
        var createdByUserId: String? = null

) : Serializable, ELFirestoreModel {

    enum class Type {

        GOOGLE_FIT;

        fun getTitle(): String {
            return when (this) {
                GOOGLE_FIT -> "Google Fit"
            }
        }

    }

    companion object {

        fun newIntegration(userId: String?,
                           type: Type,
                           serverAuthCode: String?): ELIntegration {
            val integration = ELIntegration()
            integration.uuid = UUID.randomUUID().toString()
            integration.type = type.name
            integration.serverAuthCode = serverAuthCode
            integration.createdDate = Date().time
            integration.createdByUserId = userId
            return integration
        }
    }

    override fun documentId(): String {
        return uuid!!
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map[FirestoreFields.UUID] = uuid
        map[FirestoreFields.TYPE] = type
        map["serverAuthCode"] = serverAuthCode
        map[FirestoreFields.CREATED_DATE] = createdDate
        map[FirestoreFields.CREATED_BY_USER_ID] = createdByUserId
        return map
    }

    fun convertedType(): Type? {
        return try {
            Type.valueOf(type!!)
        } catch (e: Exception) {
            null
        }
    }
}