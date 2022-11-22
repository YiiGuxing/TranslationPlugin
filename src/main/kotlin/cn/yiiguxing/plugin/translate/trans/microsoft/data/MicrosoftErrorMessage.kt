package cn.yiiguxing.plugin.translate.trans.microsoft.data

import com.google.gson.annotations.SerializedName

data class MicrosoftErrorMessage(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String
)

val MicrosoftErrorMessage.presentableError: String get() = "[$code] $message"