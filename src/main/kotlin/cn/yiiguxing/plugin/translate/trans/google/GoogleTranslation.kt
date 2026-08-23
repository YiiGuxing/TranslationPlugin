/*
 * GoogleTranslation
 */

@file:Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.trans.TranslationAdapter
import cn.yiiguxing.plugin.translate.trans.text.NamedTranslationDocument
import com.google.gson.annotations.SerializedName

data class GoogleTranslation(
    var original: String? = null,
    @SerializedName("sourceLanguage")
    val src: Lang,
    var target: Lang? = null,
    @SerializedName("translation")
    val translation: String,
    @SerializedName("sourceTransliteration")
    val sourceTransliteration: String,
    @SerializedName("targetTransliteration")
    val targetTransliteration: String,
    @SerializedName("sentences")
    val sentences: List<GSentence>?,
    @SerializedName("bilingualDictionary")
    val bilingualDictionary: List<GBilingualDictionary>?,
    @SerializedName("queryCorrection")
    val queryCorrection: GQueryCorrection?,
    @SerializedName("detectedLanguages")
    val detectedLanguages: GDetectedLanguages,
    @SerializedName("alternativeTranslations")
    val alternativeTranslations: List<GAlternativeTranslation>? = null,
    @SerializedName("sourceExamples")
    val sourceExamples: List<GSourceExample>? = null
) : TranslationAdapter {

    override fun toTranslation(): Translation {
        check(original != null) { "Cannot convert to Translation: original=null" }
        check(target != null) { "Cannot convert to Translation: target=null" }

        val extraDocuments = GoogleExampleDocumentFactory.getDocument(sourceExamples)?.let {
            listOf(NamedTranslationDocument(message("examples.document.name"), it))
        } ?: emptyList()

        return Translation(
            original!!,
            translation,
            src,
            target!!,
            detectedLanguages.srclangs,
            sourceTransliteration,
            targetTransliteration,
            queryCorrection?.spellRes,
            GoogleDictionaryDocumentFactory.getDocument(this),
            extraDocuments
        )
    }
}


data class GSentence(
    @SerializedName("orig")
    val original: String,
    @SerializedName("trans")
    val translation: String,
)

data class GBilingualDictionary(
    @SerializedName("pos")
    val pos: String,
    @SerializedName("entry")
    val entry: List<GBilingualDictionaryEntry>?
)

data class GBilingualDictionaryEntry(
    @SerializedName("word")
    val word: String,
    @SerializedName("reverseTranslation")
    val reverseTranslation: List<String>?,
    @SerializedName("score")
    val score: Float
)

data class GDetectedLanguages(
    @SerializedName("srclangs")
    val srclangs: List<Lang>,
    @SerializedName("srclangsConfidences")
    val srclangsConfidences: List<Float>,
    @SerializedName("extendedSrclangs")
    val extendedSrclangs: List<Lang>
)

data class GAlternativeTranslation(
    @SerializedName("srcPhrase")
    val srcPhrase: String,
    @SerializedName("rawSrcSegment")
    val rawSrcSegment: String,
    @SerializedName("alternative")
    val alternative: List<GAlternative>,
    @SerializedName("startPos")
    val startPos: Int,
    @SerializedName("endPos")
    val endPos: Int
)

data class GAlternative(
    @SerializedName("wordPostproc")
    val wordPostproc: String,
    @SerializedName("hasPrecedingSpace")
    val hasPrecedingSpace: Boolean,
    @SerializedName("attachToNextToken")
    val attachToNextToken: Boolean
)

data class GQueryCorrection(
    @SerializedName("spellRes")
    val spellRes: String,
    @SerializedName("spellHtmlRes")
    val spellHtmlRes: String,
    @SerializedName("correctionType")
    val correctionType: List<String>,
    @SerializedName("confident")
    val confident: Boolean
)

data class GSourceExample(
    @SerializedName("text")
    val text: String,
)