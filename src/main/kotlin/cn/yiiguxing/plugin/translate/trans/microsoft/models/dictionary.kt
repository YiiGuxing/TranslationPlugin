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


// Doc: https://learn.microsoft.com/en-us/azure/ai-services/translator/reference/v3-0-dictionary-examples


internal data class DictionaryExampleInputText(
    @SerializedName("Text") val text: String,
    @SerializedName("Translation") val translation: String,
)

internal data class DictionaryExample(
    @SerializedName("normalizedSource") val normalizedSource: String,
    @SerializedName("normalizedTarget") val normalizedTarget: String,
    @SerializedName("examples") val examples: List<DictionaryExampleItem>,
)

internal data class DictionaryExampleItem(
    @SerializedName("sourcePrefix") val sourcePrefix: String,
    @SerializedName("sourceTerm") val sourceTerm: String,
    @SerializedName("sourceSuffix") val sourceSuffix: String,
    @SerializedName("targetPrefix") val targetPrefix: String,
    @SerializedName("targetTerm") val targetTerm: String,
    @SerializedName("targetSuffix") val targetSuffix: String
)