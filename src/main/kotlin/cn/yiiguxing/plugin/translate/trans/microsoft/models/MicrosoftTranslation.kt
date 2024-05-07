package cn.yiiguxing.plugin.translate.trans.microsoft.models

import com.google.gson.annotations.SerializedName

data class MicrosoftTranslation(
    @SerializedName("sourceText")
    var sourceText: SourceText,
    @SerializedName("detectedLanguage")
    val detectedLanguage: MSDetectedLanguage? = null,
    @SerializedName("translations")
    val translations: List<MSTranslation> = emptyList()
)

data class MSDetectedLanguage(
    @SerializedName("language")
    val language: String,
    @SerializedName("score")
    val score: Float
)

data class MSTranslation(
    @SerializedName("to")
    val to: String,
    @SerializedName("text")
    val text: String
)
