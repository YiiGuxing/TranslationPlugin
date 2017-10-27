@file:Suppress("ArrayInDataClass", "unused")

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
        val phoneticSymbol: Symbol? = null,
        val dictionaries: Array<Dict> = emptyArray(),
        val otherExplain: Map<String, Array<String>> = emptyMap()
)

/**
 * Phonetic Symbol
 */
data class Symbol(val src: String?, val trans: String?)

/**
 * Dictionary
 */
data class Dict(
        val partOfSpeech: String,
        val terms: Array<String> = emptyArray(),
        val entries: Array<DictEntry> = emptyArray()
)

/**
 * Entry of Dictionary
 */
data class DictEntry(val word: String, val reverseTranslation: Array<String> = emptyArray())