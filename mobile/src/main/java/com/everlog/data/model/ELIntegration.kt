package com.everlog.data.model

import com.everlog.R
import com.everlog.constants.ELConstants
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

        fun getIcon(): Int? {
            return when (this) {
                GOOGLE_FIT -> R.drawable.ic_integration_google_fit
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
        map[ELConstants.FIELD_UUID] = uuid
        map[ELConstants.FIELD_TYPE] = type
        map["serverAuthCode"] = serverAuthCode
        map[ELConstants.FIELD_CREATED_DATE] = createdDate
        map[ELConstants.FIELD_CREATED_BY_USER_ID] = createdByUserId
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