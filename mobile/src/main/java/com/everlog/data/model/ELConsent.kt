package com.everlog.data.model

import com.everlog.constants.ELConstants
import java.io.Serializable
import java.util.*

data class ELConsent (

        var uuid: String? = null,
        var createdByUserId: String? = null,

        // Newsletter

        private var newsletter: Boolean? = null,
        private var newsletterDate: Long = 0

) : Serializable, ELFirestoreModel {

    companion object {

        @JvmStatic
        fun newConsent(userId: String?, newsletter: Boolean): ELConsent {
            val consent = ELConsent()
            consent.uuid = userId // Use this userId here to make sure we only have one consent model per user
            consent.createdByUserId = userId
            consent.updateNewsletter(newsletter)
            return consent
        }
    }

    override fun documentId(): String {
        return uuid!!
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map[ELConstants.FIELD_UUID] = uuid
        map[ELConstants.FIELD_CREATED_BY_USER_ID] = createdByUserId
        map["newsletter"] = newsletter
        map["newsletterDate"] = newsletterDate
        return map
    }

    fun getNewsletter(): Boolean? {
        return newsletter
    }

    fun updateNewsletter(value: Boolean) {
        newsletter = value
        newsletterDate = Date().time
    }
}