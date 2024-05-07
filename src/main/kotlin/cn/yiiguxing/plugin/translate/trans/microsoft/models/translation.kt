package cn.yiiguxing.plugin.translate.trans.microsoft.models

import com.google.gson.annotations.SerializedName

// Doc: https://learn.microsoft.com/en-us/azure/ai-services/translator/reference/v3-0-translate

internal data class InputText(@SerializedName("Text") val text: String)

internal data class MicrosoftTranslation(
    @SerializedName("sourceText") var sourceText: SourceText? = null,
    @SerializedName("detectedLanguage") val detectedLanguage: DetectedLanguage? = null,
    @SerializedName("translations") val translations: List<TranslationItem> = emptyList()
)

internal data class SourceText(@SerializedName("text") val text: String)

internal data class DetectedLanguage(
    @SerializedName("language") val language: String,
    @SerializedName("score") val score: Float
)

internal data class TranslationItem(
    @SerializedName("to") val to: String,
    @SerializedName("text") val text: String
)
