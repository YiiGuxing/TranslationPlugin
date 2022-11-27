package cn.yiiguxing.plugin.translate.trans.baidu

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

/**
 * Language adapter for Baidu Translator.
 */
object BaiduLanguageAdapter : BaseLanguageAdapter() {

    /** 通用版支持的语言列表 */
    private val SUPPORTED_LANGUAGES: List<Lang> = listOf(
        Lang.CHINESE,
        Lang.ENGLISH,
        Lang.CHINESE_TRADITIONAL,
        Lang.CHINESE_CANTONESE,
        Lang.CHINESE_CLASSICAL,
        Lang.JAPANESE,
        Lang.KOREAN,
        Lang.FRENCH,
        Lang.SPANISH,
        Lang.THAI,
        Lang.ARABIC,
        Lang.RUSSIAN,
        Lang.PORTUGUESE,
        Lang.GERMAN,
        Lang.ITALIAN,
        Lang.GREEK,
        Lang.DUTCH,
        Lang.POLISH,
        Lang.BULGARIAN,
        Lang.ESTONIAN,
        Lang.DANISH,
        Lang.FINNISH,
        Lang.CZECH,
        Lang.ROMANIAN,
        Lang.SLOVENIAN,
        Lang.SWEDISH,
        Lang.HUNGARIAN,
        Lang.VIETNAMESE
    )

    /** 尊享版支持的语言列表 */
    @Suppress("unused")
    private val SUPPORTED_LANGUAGES_PRO: List<Lang> = listOf(
        Lang.CHINESE,
        Lang.ENGLISH,
        Lang.CHINESE_TRADITIONAL,
        Lang.CHINESE_CANTONESE,
        Lang.CHINESE_CLASSICAL,
        Lang.ALBANIAN,
        Lang.AMHARIC,
        Lang.ARABIC,
        Lang.AZERBAIJANI,
        Lang.IRISH,
        Lang.ESTONIAN,
        Lang.BASQUE,
        Lang.BELARUSIAN,
        Lang.BOSNIAN,
        Lang.BULGARIAN,
        Lang.PORTUGUESE,
        Lang.POLISH,
        Lang.PERSIAN,
        Lang.ICELANDIC,
        Lang.DANISH,
        Lang.GERMAN,
        Lang.GEORGIAN,
        Lang.GUJARATI,
        Lang.KHMER,
        Lang.KOREAN,
        Lang.JAPANESE,
        Lang.FILIPINO,
        Lang.FINNISH,
        Lang.FRENCH,
        Lang.DUTCH,
        Lang.GALICIAN,
        Lang.CATALAN,
        Lang.RUSSIAN,
        Lang.CZECH,
        Lang.KANNADA,
        Lang.XHOSA,
        Lang.CROATIAN,
        Lang.KURDISH,
        Lang.ROMANIAN,
        Lang.LATIN,
        Lang.LATVIAN,
        Lang.KINYARWANDA,
        Lang.LITHUANIAN,
        Lang.MALAY,
        Lang.MYANMAR,
        Lang.MALAYALAM,
        Lang.MACEDONIAN,
        Lang.BENGALI,
        Lang.MALTESE,
        Lang.NORWEGIAN,
        Lang.AFRIKAANS,
        Lang.NEPALI,
        Lang.PUNJABI,
        Lang.SWEDISH,
        Lang.SERBIAN,
        Lang.SINHALA,
        Lang.ESPERANTO,
        Lang.SLOVAK,
        Lang.SLOVENIAN,
        Lang.SWAHILI,
        Lang.SOMALI,
        Lang.THAI,
        Lang.TURKISH,
        Lang.TAJIK,
        Lang.TAMIL,
        Lang.TELUGU,
        Lang.UKRAINIAN,
        Lang.WELSH,
        Lang.URDU,
        Lang.SPANISH,
        Lang.HEBREW,
        Lang.GREEK,
        Lang.HUNGARIAN,
        Lang.HINDI,
        Lang.INDONESIAN,
        Lang.ITALIAN,
        Lang.VIETNAMESE,
        Lang.ARMENIAN,
    )

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES
        .toMutableList()
        .apply { add(0, Lang.AUTO) }

    override val supportedTargetLanguages: List<Lang> = SUPPORTED_LANGUAGES

    override fun getAdaptedLanguages(): Map<String, Lang> = mapOf(
        "afr" to Lang.AFRIKAANS,
        "alb" to Lang.ALBANIAN,
        "amh" to Lang.AMHARIC,
        "ara" to Lang.ARABIC,
        "arm" to Lang.ARMENIAN,
        "aze" to Lang.AZERBAIJANI,
        "baq" to Lang.BASQUE,
        "bel" to Lang.BELARUSIAN,
        "ben" to Lang.BENGALI,
        "bos" to Lang.BOSNIAN,
        "bul" to Lang.BULGARIAN,
        "zh" to Lang.CHINESE,
        "cht" to Lang.CHINESE_TRADITIONAL,
        "wyw" to Lang.CHINESE_CLASSICAL,
        "yue" to Lang.CHINESE_CANTONESE,
        "cat" to Lang.CATALAN,
        "hrv" to Lang.CROATIAN,
        "dan" to Lang.DANISH,
        "epo" to Lang.ESPERANTO,
        "est" to Lang.ESTONIAN,
        "fil" to Lang.FILIPINO,
        "fin" to Lang.FINNISH,
        "fra" to Lang.FRENCH,
        "fry" to Lang.FRISIAN,
        "glg" to Lang.GALICIAN,
        "geo" to Lang.GEORGIAN,
        "guj" to Lang.GUJARATI,
        "heb" to Lang.HEBREW,
        "ice" to Lang.ICELANDIC,
        "gle" to Lang.IRISH,
        "jp" to Lang.JAPANESE,
        "kan" to Lang.KANNADA,
        "khm" to Lang.KHMER,
        "kin" to Lang.KINYARWANDA,
        "kor" to Lang.KOREAN,
        "kur" to Lang.KURDISH,
        "lat" to Lang.LATIN,
        "lav" to Lang.LATVIAN,
        "lit" to Lang.LITHUANIAN,
        "mac" to Lang.MACEDONIAN,
        "may" to Lang.MALAY,
        "mlt" to Lang.MALTESE,
        "bur" to Lang.MYANMAR,
        "mal" to Lang.MALAYALAM,
        "nep" to Lang.NEPALI,
        "nor" to Lang.NORWEGIAN,
        "per" to Lang.PERSIAN,
        "pan" to Lang.PUNJABI,
        "rom" to Lang.ROMANIAN,
        "srp" to Lang.SERBIAN,
        "sin" to Lang.SINHALA,
        "sk" to Lang.SLOVAK,
        "slo" to Lang.SLOVENIAN,
        "som" to Lang.SOMALI,
        "spa" to Lang.SPANISH,
        "swa" to Lang.SWAHILI,
        "swe" to Lang.SWEDISH,
        "tgk" to Lang.TAJIK,
        "tam" to Lang.TAMIL,
        "tel" to Lang.TELUGU,
        "ukr" to Lang.UKRAINIAN,
        "urd" to Lang.URDU,
        "vie" to Lang.VIETNAMESE,
        "wel" to Lang.WELSH,
        "xho" to Lang.XHOSA,
    )

}


/**
 * Language code for Baidu Translator.
 */
val Lang.baiduLanguageCode: String
    get() = BaiduLanguageAdapter.getLanguageCode(this)

/**
 * Returns the [language][Lang] for the specified Baidu Translator language [code].
 */
fun Lang.Companion.fromBaiduLanguageCode(code: String): Lang {
    return BaiduLanguageAdapter.getLanguage(code)
}