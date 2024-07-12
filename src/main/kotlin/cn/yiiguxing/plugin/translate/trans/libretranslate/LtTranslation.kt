package cn.yiiguxing.plugin.translate.trans.libretranslate

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.TranslationAdapter
import com.google.gson.annotations.SerializedName

@Suppress("MemberVisibilityCanBePrivate")
data class LtTranslation(
    var query: String = "",
    var src: Lang,
    var target: Lang,
    @SerializedName("detectedLanguage")
    val detectedLanguage: DetectedLanguage,
    @SerializedName("translatedText")
    val translatedText: String,
    @SerializedName("error")
    val error: String,
) : TranslationAdapter {
    val isSuccessful get() = error.isNullOrEmpty()

    override fun toTranslation(): Translation {
        check(isSuccessful) { "Cannot convert to Translation: error=${error}" }

        val translation = translatedText.takeIf { it.isNotEmpty() } ?: query
        return Translation(query, translation, src, target, listOf(src))
    }
}

data class DetectedLanguage(
    @SerializedName("confidence")
    val confidence: Float,
    @SerializedName("language")
    val language: String
)
