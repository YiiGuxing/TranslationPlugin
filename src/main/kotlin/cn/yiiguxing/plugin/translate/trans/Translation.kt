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
        val srcLang: Lang,
        val targetLang: Lang,
        val srcPhoneticSymbol: String? = null,
        val transPhoneticSymbol: String? = null,
        val dictionaries: List<Dict> = emptyList(),
        val otherExplain: Map<String, String> = emptyMap()
)

/**
 * Dictionary
 */
data class Dict(
        val partOfSpeech: String,
        val terms: List<String> = emptyList(),
        val entries: List<DictEntry> = emptyList()
)

/**
 * Entry of Dictionary
 */
data class DictEntry(val word: String, val reverseTranslation: List<String> = emptyList())