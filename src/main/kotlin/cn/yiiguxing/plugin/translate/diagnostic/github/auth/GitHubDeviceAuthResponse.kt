package cn.yiiguxing.plugin.translate.diagnostic.github.auth

import com.google.gson.annotations.SerializedName

internal data class GitHubDeviceAuthResponse(
    @SerializedName("error")
    val errorCode: String?,
    @SerializedName("error_description")
    val errorDescription: String?,
    @SerializedName("interval")
    val newInterval: Int?,
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("token_type")
    val tokenType: String?,
    @SerializedName("scope")
    val scope: String?
) {
    val isSuccessful: Boolean get() = errorCode == null

    fun getToken(): GitHubDeviceToken {
        check(isSuccessful) {
            "Authentication failed, unable to get token. errorCode=$errorCode, errorDescription=$errorDescription"
        }

        return GitHubDeviceToken(accessToken!!, tokenType!!, scope!!)
    }

    override fun toString(): String {
        val sb = StringBuilder()
            .append(javaClass.simpleName, "(")
        if (isSuccessful) {
            sb.append("accessToken=**********, ")
            sb.append("tokenType=", tokenType, ", ")
            sb.append("scope=", scope)
        } else {
            sb.append("errorCode=", errorCode, ", ")
            sb.append("errorDescription=", errorDescription, ", ")
            sb.append("newInterval=", newInterval)
        }
        sb.append(")")
        return sb.toString()
    }
}