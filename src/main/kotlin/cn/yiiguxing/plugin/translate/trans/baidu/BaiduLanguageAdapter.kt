package cn.yiiguxing.plugin.translate.trans.baidu

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

object BaiduLanguageAdapter : BaseLanguageAdapter() {

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

val Lang.baiduLanguageCode: String
    get() = BaiduLanguageAdapter.getLanguageCode(this)

fun Lang.Companion.fromBaiduLanguageCode(code: String): Lang {
    return BaiduLanguageAdapter.getLanguage(code)
}