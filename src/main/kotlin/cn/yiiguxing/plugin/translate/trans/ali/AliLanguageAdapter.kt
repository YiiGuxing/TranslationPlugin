package cn.yiiguxing.plugin.translate.trans.ali

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.SupportedLanguages

/**
 * Language adapter for Ali Translator.
 */
object AliLanguageAdapter : BaseLanguageAdapter(), SupportedLanguages {

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
        Lang.MALAY
    )

    override fun getAdaptedLanguages(): Map<String, Lang> = mapOf(
        "zh" to Lang.CHINESE_SIMPLIFIED,
        "zh-tw" to Lang.CHINESE_TRADITIONAL,
        "yue" to Lang.CHINESE_CANTONESE,
        "fil" to Lang.FILIPINO,
        "hbs" to Lang.CROATIAN,
        "he" to Lang.HEBREW,
    )

}


/**
 * Language code for Ali Translator.
 */
val Lang.aliLanguageCode: String
    get() = AliLanguageAdapter.getLanguageCode(this)

/**
 * Returns the [language][Lang] for the specified Ali Translator language [code].
 */
fun Lang.Companion.fromAliLanguageCode(code: String): Lang {
    return AliLanguageAdapter.getLanguage(code)
}