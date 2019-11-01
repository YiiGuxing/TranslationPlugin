package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.trans.Lang
import java.util.*

/**
 * WordBookItem
 */
data class WordBookItem(
    var id: Long?,
    val word: String,
    val sourceLanguage: Lang,
    val targetLanguage: Lang,
    val phonetic: String?,
    val explanation: String?,
    val tags: Set<String> = emptySet(),
    val createdAt: Date = Date(System.currentTimeMillis())
) {
    constructor(
        id: Long?,
        word: String,
        sourceLanguage: Lang,
        targetLanguage: Lang,
        phonetic: String?,
        explanation: String?,
        tags: String? = null,
        createdAt: Date = Date(System.currentTimeMillis())
    ) : this(id, word, sourceLanguage, targetLanguage, phonetic, explanation, tags.toTagSet(), createdAt)
}

val REGEX_TAGS_SEPARATOR = Regex("(\\s*[,，]\\s*)+")
private val REGEX_WHITESPACE = Regex("[\\s ]+")

fun String?.toTagSet(): Set<String> {
    return this?.trim(' ', ',', '，', ' ' /* 0xA0 */)
        ?.takeIf { it.isNotEmpty() }
        ?.replace(REGEX_WHITESPACE, " ")
        ?.split(REGEX_TAGS_SEPARATOR)
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        ?: emptySet()
}