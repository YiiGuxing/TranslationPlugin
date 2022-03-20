package cn.yiiguxing.plugin.translate.diagnostic.github.auth

import com.google.gson.annotations.SerializedName

internal data class GitHubUser(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
) {

    companion object {
        val UNKNOWN_USER = GitHubUser(-1, "Unknown")
    }

}
