package cn.yiiguxing.plugin.translate.trans.edge

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.TranslationAdapter
import com.google.gson.annotations.SerializedName

class EdgeTranslationResultList : ArrayList<EdgeTranslationResult>()

data class EdgeTranslationResult(
    @SerializedName("detectedLanguage")
    val detectedLanguage: DetectedLanguage? = null,
    @SerializedName("translations")
    val translations: List<EdgeTranslation>,

    var original: String? = null,
    var srcLang: Lang? = null,
    var targetLang: Lang? = null,
) : TranslationAdapter {
    override fun toTranslation(): Translation {
        val firstTranslation = translations.firstOrNull()
        if (firstTranslation != null) {
            var targetLang1 = Lang.fromEdgeLanguageCode(firstTranslation.to)

            if (targetLang1 == Lang.UNKNOWN) {
                targetLang?.let {
                    targetLang1 = it
                }
            }

            detectedLanguage?.let {
                srcLang = Lang.fromEdgeLanguageCode(it.language)
            }

            return Translation(
                original!!, firstTranslation.text, srcLang!!, targetLang1, listOf(srcLang!!)
            )
        }
        return Translation(original!!, original, srcLang!!, targetLang!!, listOf(srcLang!!))
    }
}

data class EdgeTranslation(
    @SerializedName("text")
    val text: String,
    @SerializedName("to")
    val to: String
)

data class EdgeRequestJsonItem(
    @SerializedName("Text")
    val text: String
)

data class DetectedLanguage(
    @SerializedName("language")
    val language: String,
    @SerializedName("score")
    val score: Double
)

