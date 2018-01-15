/*
 * GoogleTranslation
 *
 * Created by Yii.Guxing on 2018/01/06
 */

@file:Suppress("MemberVisibilityCanPrivate")

package cn.yiiguxing.plugin.translate.trans

import com.google.gson.annotations.SerializedName


data class GoogleTranslation(
        var original: String? = null,
        val src: Lang,
        var target: Lang? = null,
        val sentences: List<GSentence>,
        val dict: List<GDict>?,
        @SerializedName("ld_result") val ldResult: LDResult
) : TranslationAdapter {

    override fun toTranslation(): Translation {
        check(original != null) { "Can not convert to Translation: original=null" }
        check(target != null) { "Can not convert to Translation: target=null" }

        val translit: TranslitSentence? = sentences.find { it is TranslitSentence } as? TranslitSentence
        val trans = sentences.mapNotNull { (it as? TransSentence)?.trans }.joinToString()

        val dictionaries = dict?.map {
            val entries = it.entry.map { DictEntry(it.word, it.reverseTranslation) }
            Dict(it.pos, it.terms, entries)
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
data class TransSentence(val orig: String, val trans: String, val backend: Int) : GSentence()
data class TranslitSentence(
        @SerializedName("src_translit") val srcTranslit: String?,
        val translit: String?
) : GSentence()

data class GDict(val pos: String, val terms: List<String>, val entry: List<GDictEntry>)
data class GDictEntry(
        val word: String,
        @SerializedName("reverse_translation") val reverseTranslation: List<String>,
        val score: Float
)

data class LDResult(
        val srclangs: List<Lang>,
        @SerializedName("srclangs_confidences") val srclangsConfidences: List<Float>
)