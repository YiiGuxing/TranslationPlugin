package cn.yiiguxing.plugin.translate.trans.ali.models

import com.google.gson.annotations.SerializedName

internal data class AliTranslationInput(
    @SerializedName("SourceText")
    val sourceText: String,
    @SerializedName("SourceLanguage")
    val sourceLanguage: String,
    @SerializedName("TargetLanguage")
    val targetLanguage: String,
    @SerializedName("FormatType")
    val formatType: String = "text",
    @SerializedName("Scene")
    val scene: String = "general"
) {
    fun toDataForm(): Map<String, String> {
        return mapOf(
            "SourceText" to sourceText,
            "SourceLanguage" to sourceLanguage,
            "TargetLanguage" to targetLanguage,
            "FormatType" to formatType,
            "Scene" to scene
        )
    }
}