package cn.yiiguxing.plugin.translate.trans.ali

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.TranslationAdapter
import com.google.gson.annotations.SerializedName

@Suppress("MemberVisibilityCanBePrivate")
data class AliTranslation(
    var query: String = "",
    var src: Lang,
    var target: Lang,
    @SerializedName("RequestId")
    val requestId: String,
    @SerializedName("Code")
    val code: String = "-1",
    @SerializedName("Data")
    val data: AliTranslationData,
    @SerializedName("Message")
    val errorMessage: String? = null,
) : TranslationAdapter {
    val isSuccessful get() = code == "200"

    val intCode get() = code.toIntOrNull() ?: -1

    override fun toTranslation(): Translation {
        check(isSuccessful) { "Cannot convert to Translation: errorCode=${code}" }

        val translation = data.translation.takeIf { it.isNotEmpty() } ?: query
        val srcLang = data.detectedLanguage?.let { Lang.fromAliLanguageCode(it) } ?: src
        return Translation(query, translation, srcLang, target, listOf(srcLang))
    }
}

data class AliTranslationData(
    @SerializedName("DetectedLanguage")
    val detectedLanguage: String? = null,
    @SerializedName("Translated")
    val translation: String
)
