package cn.yiiguxing.plugin.translate.trans.openai.exception

import com.google.gson.annotations.SerializedName

/**
 * Represents an error object returned by the OpenAI API.
 *
 * @param code error code returned by the OpenAI API.
 * @param message human-readable error message describing the error that occurred.
 * @param param the parameter that caused the error, if applicable.
 * @param type the type of error that occurred.
 */
data class OpenAIError internal constructor(
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("param") val param: String?,
    @SerializedName("type") val type: String?,
)
