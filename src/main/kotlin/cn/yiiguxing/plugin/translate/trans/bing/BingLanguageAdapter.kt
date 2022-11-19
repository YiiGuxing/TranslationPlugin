package cn.yiiguxing.plugin.translate.trans.bing

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

/**
 * Language adapter for Bing Translator.
 */
object BingLanguageAdapter : BaseLanguageAdapter() {

    private val UNSUPPORTED_LANGUAGES: Set<Lang> = setOf(
        Lang.BELARUSIAN,
        Lang.CEBUANO,
        Lang.CHICHEWA,
        Lang.CORSICAN,
        Lang.ENGLISH_AMERICAN,
        Lang.ENGLISH_BRITISH,
        Lang.ESPERANTO,
        Lang.FRISIAN,
        Lang.HAUSA,
        Lang.HAWAIIAN,
        Lang.IGBO,
        Lang.JAVANESE,
        Lang.KINYARWANDA,
        Lang.LATIN,
        Lang.LUXEMBOURGISH,
        Lang.PORTUGUESE,
        Lang.SERBIAN,
        Lang.SESOTHO,
        Lang.SHONA,
        Lang.SINDHI,
        Lang.SINHALA,
        Lang.SUNDANESE,
        Lang.TAJIK,
        Lang.XHOSA,
        Lang.YIDDISH,
        Lang.YORUBA,
    )

    override fun getAdaptedLanguages(): Map<String, Lang> = mapOf(
        "yue" to Lang.CHINESE_CANTONESE,
        "lzh" to Lang.CHINESE_CLASSICAL,
        "zh-Hans" to Lang.CHINESE,
        "zh-Hant" to Lang.CHINESE_TRADITIONAL,
        "fil" to Lang.FILIPINO,
        "he" to Lang.HEBREW,
        "mww" to Lang.HMONG,
        "mn-Mong" to Lang.MONGOLIAN,
        "nb" to Lang.NORWEGIAN,
        "pt" to Lang.PORTUGUESE_BRAZILIAN,
    )

    override val supportedSourceLanguages: List<Lang> = (Lang.sortedLanguages - UNSUPPORTED_LANGUAGES).toList()

    override val supportedTargetLanguages: List<Lang> =
        (Lang.sortedLanguages - UNSUPPORTED_LANGUAGES - Lang.AUTO).toList()
}


/**
 * Language code for Bing Translator.
 */
val Lang.bingLanguageCode: String
    get() = BingLanguageAdapter.getLanguageCode(this)

/**
 * Returns the [language][Lang] for the specified Bing Translator language [code].
 */
fun Lang.Companion.fromBingLanguageCode(code: String): Lang {
    return BingLanguageAdapter.getLanguage(code)
}