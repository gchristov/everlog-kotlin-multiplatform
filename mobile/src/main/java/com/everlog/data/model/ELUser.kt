package com.everlog.data.model

import android.text.TextUtils
import com.everlog.constants.ELConstants
import com.everlog.data.model.pro.ELProSubscription
import com.google.firebase.auth.FirebaseUser
import java.io.Serializable
import java.util.*

data class ELUser (

        var id: String? = null,
        var displayName: String? = null,
        var email: String? = null,
        var photoUrl: String? = null,
        var createdDate: Long = 0,

        // Pro

        var subscription: ELProSubscription? = null

) : Serializable, ELFirestoreModel {

    companion object {

        @JvmStatic
        fun buildUser(account: FirebaseUser): ELUser {
            val user = ELUser()
            user.id = account.uid
            user.displayName = account.displayName
            user.email = account.email
            if (account.photoUrl != null) {
                user.photoUrl = account.photoUrl.toString()
            }
            user.createdDate = account.metadata?.creationTimestamp ?: 0
            return user
        }
    }

    override fun documentId(): String {
        return id!!
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map["id"] = id
        map["displayName"] = displayName
        map["email"] = email
        map["photoUrl"] = photoUrl
        map[ELConstants.FIELD_CREATED_DATE] = createdDate
        if (subscription != null) {
            map["subscription"] = subscription?.asMap()
        }
        return map
    }

    fun getFirstName(): String? {
        if (!TextUtils.isEmpty(displayName)) {
            val split = displayName!!.split(" ").toTypedArray()
            return split[0]
        }
        return null
    }

    fun getLastName(): String? {
        if (!TextUtils.isEmpty(displayName)) {
            val split = displayName!!.split(" ").toTypedArray()
            if (split.size > 1) {
                return split[1]
            }
        }
        return null
    }

    fun isPro(): Boolean {
        return true
        // Todo: Edited as per #181
//        return subscription?.isPro() == true
    }

    fun isProWithinFreeTrial(): Boolean {
        return false
        // Todo: Edited as per #181
//        return subscription?.isProWithinFreeTrial() == true
    }

    fun proFreeTrialDaysRemaining(): Int {
        return -1
        // Todo: Edited as per #181
//        return subscription?.proFreeTrialDaysRemaining() ?: -1
    }
}