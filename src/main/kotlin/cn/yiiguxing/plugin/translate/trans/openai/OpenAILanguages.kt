package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.trans.Lang

internal object OpenAILanguages {

    val languageMap = linkedMapOf(
        Lang.ENGLISH to "English",
        Lang.CHINESE to "Simplified Chinese",
        Lang.CHINESE_TRADITIONAL to "Traditional Chinese",
        Lang.JAPANESE to "Japanese",
        Lang.KOREAN to "Korean",
        Lang.RUSSIAN to "Russian",
        Lang.GERMAN to "German",
        Lang.FRENCH to "French",
        Lang.SPANISH to "Spanish",
        Lang.ARABIC to "Arabic",
        Lang.HINDI to "Hindi",
        Lang.PORTUGUESE to "Portuguese",
        Lang.BENGALI to "Bengali",
        Lang.TURKISH to "Turkish",
        Lang.ITALIAN to "Italian",
    )

    val languages = languageMap.keys.toList()

}

/**
 * The language name of the OpenAI API.
 * @see OpenAILanguages.languageMap
 */
val Lang.openAILanguage: String
    get() = OpenAILanguages.languageMap[this] ?: throw IllegalArgumentException("Unsupported language: $this")