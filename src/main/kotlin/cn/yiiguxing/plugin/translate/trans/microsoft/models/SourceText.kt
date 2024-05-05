package cn.yiiguxing.plugin.translate.trans.microsoft.models

import com.google.gson.annotations.SerializedName

data class SourceText(
    @SerializedName("Text") val text: String
)
