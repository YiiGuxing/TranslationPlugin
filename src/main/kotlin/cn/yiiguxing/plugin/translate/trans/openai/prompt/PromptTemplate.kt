package cn.yiiguxing.plugin.translate.trans.openai.prompt

import cn.yiiguxing.plugin.translate.trans.Lang

/**
 * Prompt template.
 *
 * @property template The template string.
 * @property sourceText The text to be translated.
 * @property sourceLanguage The [source language][Lang] of the [sourceText] to be translated.
 * @property targetLanguage The [target language][Lang] to be translated into.
 */
data class PromptTemplate(
    val template: String,
    val sourceText: String,
    val sourceLanguage: Lang,
    val targetLanguage: Lang,
)