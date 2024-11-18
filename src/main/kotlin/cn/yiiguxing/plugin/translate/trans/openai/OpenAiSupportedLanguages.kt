package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.SupportedLanguages

internal object OpenAiSupportedLanguages : SupportedLanguages {

    private val unsupportedLanguages: Set<Lang> = setOf(
        Lang.UNKNOWN,
        Lang.CHINESE,
        Lang.CHINESE_CANTONESE,
        Lang.CHINESE_CLASSICAL,
        Lang.ENGLISH_AMERICAN,
        Lang.ENGLISH_BRITISH,
        Lang.FRENCH_CANADA,
        Lang.KURDISH_KURMANJI,
        Lang.KURDISH_SORANI,
        Lang.PORTUGUESE_BRAZILIAN,
        Lang.PORTUGUESE_PORTUGUESE,
        Lang.SERBIAN_CYRILLIC,
        Lang.SERBIAN_LATIN,
    )

    override val sourceLanguages: List<Lang> by lazy {
        (Lang.sortedLanguages - unsupportedLanguages).toList()
    }

    override val targetLanguages: List<Lang> by lazy {
        (Lang.sortedLanguages - unsupportedLanguages.toMutableSet().apply { add(Lang.AUTO) }).toList()
    }
}