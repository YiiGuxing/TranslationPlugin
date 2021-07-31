package cn.yiiguxing.plugin.translate.trans

import com.google.gson.annotations.SerializedName

@Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")
data class AliTranslation(
    var query: String = "",
    var src: Lang,
    var target: Lang,
    @SerializedName("RequestId")
    val requestId: String,
    @SerializedName("Code")
    val code: Int = 0,
    @SerializedName("Data")
    val data: AliTranslationData,
    @SerializedName("Message")
    val errorMessage: String,
) : TranslationAdapter {
    val isSuccessful get() = code == 200

    override fun toTranslation(): Translation {
        check(isSuccessful) { "Cannot convert to Translation: errorCode=${code}" }
        check(data.translation.isNotEmpty()) { "Cannot convert to Translation: data.translation = null" }

        return Translation(query, data.translation, src, target, listOf(target))
    }
}

data class AliTranslationData(
    @SerializedName("WordCount")
    val wordCount: Int? = 0,
    @SerializedName("Translated")
    val translation: String
)

/**
 * 序列化json模型
 */
@Suppress("MemberVisibilityCanBePrivate")
data class AliTranslationRequest constructor(
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
)
