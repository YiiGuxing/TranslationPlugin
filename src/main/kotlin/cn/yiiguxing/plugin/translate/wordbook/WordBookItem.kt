package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translation
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
    ) : this(
        id,
        word,
        sourceLanguage,
        targetLanguage,
        phonetic,
        explanation,
        tags?.let { WordTags.getTagSet(it) } ?: emptySet(),
        createdAt
    )
}

fun Translation.toWordBookItem(): WordBookItem {
    val explainsBuilder = StringBuilder()
    val dictText = dictDocument?.text ?: ""

    if (!translation.isNullOrBlank()) {
        explainsBuilder.append(translation)
        if (dictText.isNotEmpty()) {
            explainsBuilder.append("\n\n")
        }
    }
    explainsBuilder.append(dictText)

    return WordBookItem(
        null,
        original.trim(),
        srcLang,
        targetLang,
        srcTransliteration,
        explainsBuilder.toString(),
        null
    )
}
