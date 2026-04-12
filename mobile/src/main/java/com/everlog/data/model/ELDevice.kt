package com.everlog.data.model

import com.everlog.constants.ELConstants
import com.everlog.managers.DeviceManager
import com.everlog.utils.device.DeviceUtils
import java.io.Serializable
import java.util.*

data class ELDevice (

        var uuid: String? = null,
        var name: String? = null,
        var token: String? = null,
        var createdDate: Long = 0

) : Serializable, ELFirestoreModel {

    companion object {

        @JvmStatic
        fun newDevice(token: String?): ELDevice {
            val device = ELDevice()
            device.uuid = DeviceManager.manager.deviceId()
            device.createdDate = Date().time
            device.token = token
            device.name = DeviceUtils.getDeviceName()
            return device
        }
    }

    override fun documentId(): String {
        return uuid!!
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map[ELConstants.FIELD_UUID] = uuid
        map["token"] = token
        map[ELConstants.FIELD_NAME] = name
        map[ELConstants.FIELD_CREATED_DATE] = createdDate
        return map
    }
}