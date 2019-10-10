package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.trans.Lang
import java.util.*

/**
 * WordBookItem
 *
 * Created by Yii.Guxing on 2019/08/06.
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

private val REGEX_SEPARATOR = "[\\s,]+".toRegex()

private fun String?.toTagSet(): Set<String> {
    return this?.takeIf { it.isNotEmpty() }
        ?.split(REGEX_SEPARATOR)
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        ?: emptySet()
}