package cn.yiiguxing.plugin.translate.trans.ali.models

import com.google.gson.annotations.SerializedName

internal data class AliTranslation(
    @SerializedName("DetectedLanguage")
    val detectedLanguage: String? = null,
    @SerializedName("Translated")
    val translated: String
)