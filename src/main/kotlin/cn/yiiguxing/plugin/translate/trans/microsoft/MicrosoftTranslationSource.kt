package cn.yiiguxing.plugin.translate.trans.microsoft

import com.google.gson.annotations.SerializedName

data class MicrosoftTranslationSource(
    @SerializedName("Text")
    val text: String
)
