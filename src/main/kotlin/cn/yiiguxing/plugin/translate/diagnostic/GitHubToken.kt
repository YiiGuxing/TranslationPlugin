package cn.yiiguxing.plugin.translate.diagnostic

import com.google.gson.annotations.SerializedName

internal data class GitHubToken(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("scope")
    val scope: String
)
