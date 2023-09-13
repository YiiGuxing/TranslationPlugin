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

private val ZERO_WIDTH_SPACE = Regex("​+")

data class GoogleTranslation(
    var original: String? = null,
    @SerializedName("src")
    val src: Lang,
    var target: Lang? = null,
    @SerializedName("sentences")
    val sentences: List<GSentence>?,
    @SerializedName("dict")
    val dict: List<GDict>?,
    @SerializedName("spell")
    val spell: GSpell?,
    @SerializedName("ld_result")
    val languageDetectionResult: GLanguageDetectionResult,
    @SerializedName("alternative_translations")
    val alternativeTranslations: List<GAlternativeTranslations>? = null,
    @SerializedName("examples")
    val examples: GExamples? = null
) : TranslationAdapter {

    override fun toTranslation(): Translation {
        check(original != null) { "Cannot convert to Translation: original=null" }
        check(target != null) { "Cannot convert to Translation: target=null" }

        val sentences = sentences ?: emptyList()
        val translit: GTranslitSentence? = sentences.find { it is GTranslitSentence } as? GTranslitSentence
        val trans = sentences.asSequence()
            .mapNotNull { (it as? GTransSentence)?.trans }
            .joinToString("")
            .replace(ZERO_WIDTH_SPACE, "")

        val extraDocuments = GoogleExamplesDocument.getDocument(examples)?.let {
            listOf(NamedTranslationDocument(message("title.google.document.examples"), it))
        } ?: emptyList()

        return Translation(
            original!!,
            trans,
            src,
            target!!,
            languageDetectionResult.srclangs,
            translit?.srcTranslit,
            translit?.translit,
            spell?.spell,
            GoogleDictDocument.Factory.getDocument(this),
            extraDocuments
        )
    }
}

sealed class GSentence

data class GTransSentence(
    @SerializedName("orig")
    val orig: String,
    @SerializedName("trans")
    val trans: String,
    @SerializedName("backend")
    val backend: Int
) : GSentence()

data class GTranslitSentence(
    @SerializedName("src_translit")
    val srcTranslit: String?,
    @SerializedName("translit")
    val translit: String?
) : GSentence()

data class GDict(
    @SerializedName("pos")
    val pos: String,
    @SerializedName("entry")
    val entry: List<GDictEntry>?
)

data class GDictEntry(
    @SerializedName("word")
    val word: String,
    @SerializedName("reverse_translation")
    val reverseTranslation: List<String>?,
    @SerializedName("score")
    val score: Float
)

data class GLanguageDetectionResult(
    @SerializedName("srclangs")
    val srclangs: List<Lang>,
    @SerializedName("srclangs_confidences")
    val srclangsConfidences: List<Float>
)

//region For dt=at
data class GAlternativeTranslations(
    @SerializedName("src_phrase")
    val srcPhrase: String,
    @SerializedName("raw_src_segment")
    val rawSrcSegment: String,
    @SerializedName("alternative")
    val alternative: List<GAlternative>
)

data class GAlternative(
    @SerializedName("word_postproc")
    val wordPostproc: String,
    @SerializedName("score")
    val score: Float,
    @SerializedName("has_preceding_space")
    val hasPrecedingSpace: Boolean,
    @SerializedName("attach_to_next_token")
    val attachToNextToken: Boolean
)
//endregion

//region For dt=qca
data class GSpell(
    @SerializedName("spell_res")
    val spell: String
)
//endregion


//region For dt=ex
data class GExamples(
    @SerializedName("example")
    val examples: List<GExample>
)

data class GExample(
    @SerializedName("text")
    val text: String,
    @SerializedName("source_type")
    val sourceType: Int,
    @SerializedName("label_info")
    val labelInfo: GLabelInfo? = null
)

data class GLabelInfo(
    @SerializedName("subject")
    val subject: List<String>
)
//endregion