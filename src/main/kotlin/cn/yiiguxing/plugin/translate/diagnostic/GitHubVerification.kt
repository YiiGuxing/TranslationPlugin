package cn.yiiguxing.plugin.translate.diagnostic

import com.google.gson.annotations.SerializedName

internal data class GitHubVerification(
    @SerializedName("device_code")
    val deviceCode: String,
    @SerializedName("user_code")
    val userCode: String,
    @SerializedName("verification_uri")
    val verificationUri: String,
    @SerializedName("expires_in")
    val expiresIn: Int,
    @SerializedName("interval")
    val interval: Int
)
