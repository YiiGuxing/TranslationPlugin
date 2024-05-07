package cn.yiiguxing.plugin.translate.trans.microsoft.models

import com.google.gson.annotations.SerializedName

// Doc: https://learn.microsoft.com/en-us/azure/ai-services/translator/reference/v3-0-dictionary-lookup


internal data class DictionaryLookup(
    @SerializedName("normalizedSource") val normalizedSource: String,
    @SerializedName("displaySource") val displaySource: String,
    @SerializedName("translations") val translations: List<DictionaryTranslation>,
)

internal data class DictionaryTranslation(
    @SerializedName("normalizedTarget") val normalizedTarget: String,
    @SerializedName("displayTarget") val displayTarget: String,
    @SerializedName("posTag") val posTag: PosTag,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("prefixWord") val prefixWord: String,
    @SerializedName("backTranslations") val backTranslations: List<DictionaryBackTranslation>,
)

internal data class DictionaryBackTranslation(
    @SerializedName("normalizedText") val normalizedText: String,
    @SerializedName("displayText") val displayText: String,
)