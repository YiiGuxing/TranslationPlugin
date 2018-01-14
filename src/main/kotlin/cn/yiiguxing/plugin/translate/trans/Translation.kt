@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans

interface TranslationAdapter {
    fun toTranslation(): Translation
}

/**
 * Translation
 */
data class Translation(
        val original: String,
        val trans: String?,
        val srcLang: Lang,
        val targetLang: Lang,
        val srclangs: List<Lang>,
        val srcTransliteration: String? = null,
        val transliteration: String? = null,
        val dictionaries: List<Dict> = emptyList(),
        val basicExplains: List<String> = emptyList(),
        val otherExplains: Map<String, String> = emptyMap())

/**
 * Dictionary
 */
data class Dict(
        val partOfSpeech: String,
        val terms: List<String> = emptyList(),
        val entries: List<DictEntry> = emptyList())

/**
 * Entry of Dictionary
 */
data class DictEntry(val word: String, val reverseTranslation: List<String> = emptyList())