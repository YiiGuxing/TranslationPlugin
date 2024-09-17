package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.SupportedLanguages

/**
 * Language adapter for Google Translator.
 */
object GoogleLanguageAdapter : BaseLanguageAdapter(), SupportedLanguages {

    private val UNSUPPORTED_LANGUAGES: Set<Lang> = setOf(
        Lang.ASSAMESE,
        Lang.BASHKIR,
        Lang.CHINESE, // 己俱体分为 `中文（简体）`
        Lang.CHINESE_CANTONESE,
        Lang.CHINESE_CLASSICAL,
        Lang.DARI,
        Lang.DIVEHI,
        Lang.ENGLISH_AMERICAN,
        Lang.ENGLISH_BRITISH,
        Lang.FAROESE,
        Lang.FIJIAN,
        Lang.FRENCH_CANADA,
        Lang.KURDISH, // 己俱体分为 `库尔德语（库尔曼吉语）` 和 `库尔德语（索拉尼）`
        Lang.PORTUGUESE_BRAZILIAN,
        Lang.PORTUGUESE_PORTUGUESE,
        Lang.SERBIAN_CYRILLIC,
        Lang.SERBIAN_LATIN,
        Lang.TAHITIAN,
        Lang.TIBETAN,
        Lang.TIGRINYA,
        Lang.TONGAN,
        Lang.UPPER_SORBIAN,
        Lang.YUCATEC_MAYA,
    )

    override fun getAdaptedLanguages(): Map<String, Lang> = mapOf()

    override val sourceLanguages: List<Lang> = (Lang.sortedLanguages - UNSUPPORTED_LANGUAGES).toList()

    override val targetLanguages: List<Lang> = (Lang.sortedLanguages - UNSUPPORTED_LANGUAGES - Lang.AUTO).toList()
}


/**
 * Language code for Google Translator.
 */
val Lang.googleLanguageCode: String
    get() = GoogleLanguageAdapter.getLanguageCode(this)

/**
 * Returns the [language][Lang] for the specified Google Translator language [code].
 */
fun Lang.Companion.fromGoogleLanguageCode(code: String): Lang {
    return GoogleLanguageAdapter.getLanguage(code)
}