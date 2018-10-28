/*
 * GoogleTranslation
 *
 * Created by Yii.Guxing on 2018/01/06
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package cn.yiiguxing.plugin.translate.trans

import com.google.gson.annotations.SerializedName


data class GoogleTranslation(
        var original: String? = null,
        val src: Lang,
        var target: Lang? = null,
        val sentences: List<GSentence>,
        val dict: List<GDict>?,
        @SerializedName("ld_result") val ldResult: GLDResult,
        // For dt=at
        @SerializedName("alternative_translations") val alternativeTranslations: List<GAlternativeTranslations>?
) : TranslationAdapter {

    override fun toTranslation(): Translation {
        check(original != null) { "Can not convert to Translation: original=null" }
        check(target != null) { "Can not convert to Translation: target=null" }

        val translit: GTranslitSentence? = sentences.find { it is GTranslitSentence } as? GTranslitSentence
        val trans = sentences.asSequence().mapNotNull { (it as? GTransSentence)?.trans }.joinToString("")

        val dictionaries = dict?.map { gDict ->
            val entries = gDict.entry.map { DictEntry(it.word, it.reverseTranslation ?: emptyList()) }
            Dict(gDict.pos, gDict.terms, entries)
        } ?: emptyList()

        return Translation(
                original!!,
                trans,
                src,
                target!!,
                ldResult.srclangs,
                translit?.srcTranslit,
                translit?.translit,
                dictionaries)
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