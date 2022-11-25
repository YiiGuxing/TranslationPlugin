package cn.yiiguxing.plugin.translate.trans.microsoft.data

import com.google.gson.annotations.SerializedName

data class MicrosoftError(
    @SerializedName("error")
    val error: MicrosoftErrorMessage,
)

data class MicrosoftErrorMessage(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String
)

val MicrosoftError.presentableError: String get() = error.presentableError
val MicrosoftErrorMessage.presentableError: String get() = "[$code] $message"