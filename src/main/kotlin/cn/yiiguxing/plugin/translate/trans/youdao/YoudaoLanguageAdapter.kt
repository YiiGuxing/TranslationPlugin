package cn.yiiguxing.plugin.translate.trans.youdao

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

/**
 * Language adapter for Youdao Translator.
 */
object YoudaoLanguageAdapter : BaseLanguageAdapter() {

    private val SUPPORTED_LANGUAGES: List<Lang> = listOf(
        Lang.AUTO,
        Lang.CHINESE,
        Lang.CHINESE_TRADITIONAL,
        Lang.ENGLISH,
        Lang.JAPANESE,
        Lang.KOREAN,
        Lang.DUTCH,
        Lang.VIETNAMESE,
        Lang.RUSSIAN,
        Lang.PORTUGUESE,
        Lang.ITALIAN,
        Lang.INDONESIAN,
        Lang.FRENCH,
        Lang.SPANISH,
        Lang.GERMAN,
        Lang.THAI,
        Lang.ARABIC,
    )

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES

    override val supportedTargetLanguages: List<Lang> = SUPPORTED_LANGUAGES

    override fun getAdaptedLanguages(): Map<String, Lang> = mapOf(
        "zh-CHS" to Lang.CHINESE,
        "zh-CHT" to Lang.CHINESE_TRADITIONAL,
    )

}


/**
 * Language code for Youdao Translator.
 */
val Lang.youdaoLanguageCode: String
    get() = YoudaoLanguageAdapter.getLanguageCode(this)

/**
 * Returns the [language][Lang] for the specified Youdao Translator language [code].
 */
fun Lang.Companion.fromYoudaoLanguageCode(code: String): Lang {
    return YoudaoLanguageAdapter.getLanguage(code)
}