package cn.yiiguxing.plugin.translate.diagnostic.github.issues

import com.google.gson.annotations.SerializedName

internal data class Issue(
    @SerializedName("number")
    val number: Int,
    @SerializedName("html_url")
    val htmlUrl: String
)