package cn.yiiguxing.plugin.translate.trans.tencent

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.SupportedLanguages

/**
 * Language adapter for Tencent Translator.
 */
object TencentLanguageAdapter : BaseLanguageAdapter(), SupportedLanguages {

    override val sourceLanguages: List<Lang> = listOf(
        Lang.AUTO,
        Lang.CHINESE_SIMPLIFIED,
        Lang.CHINESE_TRADITIONAL,
        Lang.ENGLISH,
        Lang.JAPANESE,
        Lang.KOREAN,
        Lang.FRENCH,
        Lang.SPANISH,
        Lang.ITALIAN,
        Lang.GERMAN,
        Lang.TURKISH,
        Lang.RUSSIAN,
        Lang.PORTUGUESE,
        Lang.VIETNAMESE,
        Lang.INDONESIAN,
        Lang.THAI,
        Lang.MALAY,
        Lang.ARABIC,
        Lang.HINDI
    )

    override val targetLanguages: List<Lang> = listOf(
        Lang.CHINESE_SIMPLIFIED,
        Lang.CHINESE_TRADITIONAL,
        Lang.ENGLISH,
        Lang.JAPANESE,
        Lang.KOREAN,
        Lang.FRENCH,
        Lang.SPANISH,
        Lang.ITALIAN,
        Lang.GERMAN,
        Lang.TURKISH,
        Lang.RUSSIAN,
        Lang.PORTUGUESE,
        Lang.VIETNAMESE,
        Lang.INDONESIAN,
        Lang.THAI,
        Lang.MALAY,
        Lang.ARABIC,
        Lang.HINDI
    )

    override fun getAdaptedLanguages(): Map<String, Lang> = mapOf(
        "zh" to Lang.CHINESE_SIMPLIFIED,
        "zh-TW" to Lang.CHINESE_TRADITIONAL,
        "en" to Lang.ENGLISH,
        "ja" to Lang.JAPANESE,
        "ko" to Lang.KOREAN,
        "fr" to Lang.FRENCH,
        "es" to Lang.SPANISH,
        "it" to Lang.ITALIAN,
        "de" to Lang.GERMAN,
        "tr" to Lang.TURKISH,
        "ru" to Lang.RUSSIAN,
        "pt" to Lang.PORTUGUESE,
        "vi" to Lang.VIETNAMESE,
        "id" to Lang.INDONESIAN,
        "th" to Lang.THAI,
        "ms" to Lang.MALAY,
        "ar" to Lang.ARABIC,
        "hi" to Lang.HINDI,
        "auto" to Lang.AUTO
    )
}

/**
 * Language code for Tencent Translator.
 */
val Lang.tencentLanguageCode: String
    get() = TencentLanguageAdapter.getLanguageCode(this)

/**
 * Returns the [language][Lang] for the specified Tencent Translator language [code].
 */
fun Lang.Companion.fromTencentLanguageCode(code: String): Lang {
    return TencentLanguageAdapter.getLanguage(code)
}