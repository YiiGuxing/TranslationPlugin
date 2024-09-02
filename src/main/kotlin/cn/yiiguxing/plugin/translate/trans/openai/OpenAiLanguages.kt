package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.trans.Lang

internal object OpenAiLanguages {

    val languageMap = linkedMapOf(
        Lang.ENGLISH to "English",
        Lang.CHINESE_SIMPLIFIED to "Simplified Chinese",
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
 * The language name of the OpenAi API.
 * @see OpenAiLanguages.languageMap
 */
val Lang.openAiLanguage: String
    get() = OpenAiLanguages.languageMap[this] ?: throw IllegalArgumentException("Unsupported language: $this")