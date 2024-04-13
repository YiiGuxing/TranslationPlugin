package cn.yiiguxing.plugin.translate.trans.microsoft.models

import com.google.gson.annotations.SerializedName

data class MicrosoftSourceText(
    @SerializedName("Text") val text: String
)
