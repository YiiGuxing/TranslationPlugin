package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

/**
 * Language adapter for Google Translator.
 */
object GoogleLanguageAdapter : BaseLanguageAdapter() {

    private val UNSUPPORTED_LANGUAGES: Set<Lang> = setOf(
        Lang.ASSAMESE,
        Lang.BASHKIR,
        Lang.CHINESE_CANTONESE,
        Lang.CHINESE_CLASSICAL,
        Lang.DARI,
        Lang.DIVEHI,
        Lang.ENGLISH_AMERICAN,
        Lang.ENGLISH_BRITISH,
        Lang.FAROESE,
        Lang.FIJIAN,
        Lang.FRENCH_CANADA,
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

    override val supportedSourceLanguages: List<Lang> = (Lang.sortedLanguages - UNSUPPORTED_LANGUAGES).toList()

    override val supportedTargetLanguages: List<Lang> =
        (Lang.sortedLanguages - UNSUPPORTED_LANGUAGES - Lang.AUTO).toList()
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