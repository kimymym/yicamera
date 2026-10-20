package com.github.kimymym.yicamera.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YiCommand(
    @SerialName("msg_id") val msgId: Int,
    @SerialName("token") val token: Int,
    @SerialName("param") val param: String? = null,
    @SerialName("type") val type: String? = null
)

@Serializable
data class YiResponse(
    @SerialName("rval") val rval: Int = 0,
    @SerialName("msg_id") val msgId: Int = 0,
    @SerialName("param") val param: JsonElement? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("listing") val listing: JsonElement? = null
)

object YiMsgIds {
    const val MSG_GET_SETTING = 1
    const val MSG_SET_SETTING = 2
    const val MSG_GET_ALL_SETTINGS = 3
    const val MSG_FORMAT_SD = 4
    const val MSG_QUERY_STORAGE = 5
    const val MSG_GET_BATTERY_STATUS = 13
    const val MSG_START_SESSION = 257
    const val MSG_STOP_SESSION = 258
    const val MSG_START_VF = 259
    const val MSG_STOP_VF = 260
    const val MSG_START_RECORD = 513
    const val MSG_STOP_RECORD = 514
    const val MSG_TAKE_PHOTO = 769
    const val MSG_LIST_FILES = 1282
}

class CameraShootException(
    val rval: Int,
    override val message: String
) : Exception(message) {
    val isSdFormatRequired: Boolean
        get() = rval == -28 || rval == -14 || rval == -24
}
