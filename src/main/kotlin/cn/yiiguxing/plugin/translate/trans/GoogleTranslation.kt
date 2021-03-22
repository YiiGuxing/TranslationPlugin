/*
 * GoogleTranslation
 */

@file:Suppress("MemberVisibilityCanBePrivate", "SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.trans.text.GoogleDictDocument
import com.google.gson.annotations.SerializedName


data class GoogleTranslation(
    var original: String? = null,
    val src: Lang,
    var target: Lang? = null,
    val sentences: List<GSentence>,
    val dict: List<GDict>?,
    val spell: GSpell?,
    @SerializedName("ld_result") val ldResult: GLDResult,
    @SerializedName("alternative_translations") val alternativeTranslations: List<GAlternativeTranslations>?
) : TranslationAdapter {

    override fun toTranslation(): Translation {
        check(original != null) { "Cannot convert to Translation: original=null" }
        check(target != null) { "Cannot convert to Translation: target=null" }

        val translit: GTranslitSentence? = sentences.find { it is GTranslitSentence } as? GTranslitSentence
        val trans = sentences.asSequence().mapNotNull { (it as? GTransSentence)?.trans }.joinToString("")

        return Translation(
            original!!,
            trans,
            src,
            target!!,
            ldResult.srclangs,
            translit?.srcTranslit,
            translit?.translit,
            spell?.spell,
            GoogleDictDocument.Factory.getDocument(this)
        )
    }
}

sealed class GSentence
data class GTransSentence(val orig: String, val trans: String, val backend: Int) : GSentence()
data class GTranslitSentence(
    @SerializedName("src_translit") val srcTranslit: String?,
    @SerializedName("translit") val translit: String?
) : GSentence()

data class GDict(val pos: String, val terms: List<String>, val entry: List<GDictEntry>)
data class GDictEntry(
    @SerializedName("word") val word: String,
    @SerializedName("reverse_translation") val reverseTranslation: List<String>?,
    @SerializedName("score") val score: Float
)

data class GLDResult(
    @SerializedName("srclangs") val srclangs: List<Lang>,
    @SerializedName("srclangs_confidences") val srclangsConfidences: List<Float>
)

// For dt=at
data class GAlternativeTranslations(
    @SerializedName("src_phrase") val srcPhrase: String,
    @SerializedName("raw_src_segment") val rawSrcSegment: String,
    @SerializedName("alternative") val alternative: List<GAlternative>
)

data class GAlternative(
    @SerializedName("word_postproc") val wordPostproc: String,
    @SerializedName("score") val score: Float,
    @SerializedName("has_preceding_space") val hasPrecedingSpace: Boolean,
    @SerializedName("attach_to_next_token") val attachToNextToken: Boolean
)

// For dt=qca
data class GSpell(@SerializedName("spell_res") val spell: String)