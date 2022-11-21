package cn.yiiguxing.plugin.translate.trans.microsoft.data

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.TranslationAdapter
import cn.yiiguxing.plugin.translate.trans.microsoft.fromMicrosoftLanguageCode

data class MicrosoftTranslation(
    var sourceText: String,
    var sourceLang: Lang,
    val detectedLanguage: MSDetectedLanguage? = null,
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

data class MSDetectedLanguage(val language: String, val score: Float)
data class MSTranslation(val to: String, val text: String)
