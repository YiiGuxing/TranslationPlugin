package cn.yiiguxing.plugin.translate.diagnostic.github.issues

import com.google.gson.annotations.SerializedName

internal data class IssueSearchResult(
    @SerializedName("items")
    val items: List<Issue>
)