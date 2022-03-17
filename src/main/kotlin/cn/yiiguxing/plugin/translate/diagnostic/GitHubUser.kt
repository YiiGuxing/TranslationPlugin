package cn.yiiguxing.plugin.translate.diagnostic

import com.google.gson.annotations.SerializedName

internal data class GitHubUser(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
)
