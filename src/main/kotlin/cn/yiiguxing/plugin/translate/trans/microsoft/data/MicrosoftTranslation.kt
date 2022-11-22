package cn.yiiguxing.plugin.translate.trans.microsoft.data

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.TranslationAdapter
import cn.yiiguxing.plugin.translate.trans.microsoft.fromMicrosoftLanguageCode
import com.google.gson.annotations.SerializedName

data class MicrosoftTranslation(
    var sourceText: String,
    var sourceLang: Lang,

    @SerializedName("detectedLanguage")
    val detectedLanguage: MSDetectedLanguage? = null,
    @SerializedName("translations")
    val translations: List<MSTranslation> = emptyList()
) : TranslationAdapter {

    override fun toTranslation(): Translation {
        val translation = translations.first()
        return Translation(
            sourceText,
            translation.text,
            detectedLanguage?.language?.let { Lang.fromMicrosoftLanguageCode(it) } ?: sourceLang,
            Lang.fromMicrosoftLanguageCode(translation.to)
        )
    }

}

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
