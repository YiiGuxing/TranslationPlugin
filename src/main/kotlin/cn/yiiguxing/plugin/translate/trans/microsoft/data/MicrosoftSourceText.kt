package cn.yiiguxing.plugin.translate.trans.microsoft.data

import com.google.gson.annotations.SerializedName

data class MicrosoftSourceText(
    @SerializedName("Text") val text: String
)
