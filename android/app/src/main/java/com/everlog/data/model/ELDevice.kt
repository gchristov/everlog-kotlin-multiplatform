package com.everlog.data.model

import com.everlog.android.common.firebase.FirestoreFields
import java.io.Serializable

data class ELDevice (

        var uuid: String? = null,
        var name: String? = null,
        var token: String? = null,
        var createdDate: Long = 0

) : Serializable, ELFirestoreModel {

    companion object {

        @JvmStatic
        fun newDevice(uuid: String?, name: String?, token: String?, createdDate: Long): ELDevice {
            val device = ELDevice()
            device.uuid = uuid
            device.createdDate = createdDate
            device.token = token
            device.name = name
            return device
        }
    }

    override fun documentId(): String {
        return uuid!!
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map[FirestoreFields.UUID] = uuid
        map["token"] = token
        map[FirestoreFields.NAME] = name
        map[FirestoreFields.CREATED_DATE] = createdDate
        return map
    }
}