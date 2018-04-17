package cn.yiiguxing.plugin.translate.trans

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey
import java.util.*

@Suppress("InvalidBundleOrProperty")
private const val LANGUAGE_BUNDLE = "messages.LanguageBundle"

private object LanguageBundle : AbstractBundle(LANGUAGE_BUNDLE)


/**
 * 语言
 */
@Suppress("InvalidBundleOrProperty")
enum class Lang(@PropertyKey(resourceBundle = LANGUAGE_BUNDLE) langNameKey: String, val code: String) {
    AUTO("auto", "auto"),
    CHINESE("chinese", "zh-CN"),
    ENGLISH("english", "en"),
    CHINESE_TRADITIONAL("chinese.traditional", "zh-TW"),
    ALBANIAN("albanian", "sq"),
    ARABIC("arabic", "ar"),
    AMHARIC("amharic", "am"),
    AZERBAIJANI("azerbaijani", "az"),
    IRISH("irish", "ga"),
    ESTONIAN("estonian", "et"),
    BASQUE("basque", "eu"),
    BELARUSIAN("belarusian", "be"),
    BULGARIAN("bulgarian", "bg"),
    ICELANDIC("icelandic", "is"),
    POLISH("polish", "pl"),
    BOSNIAN("bosnian", "bs"),
    PERSIAN("persian", "fa"),
    AFRIKAANS("afrikaans", "af"),
    DANISH("danish", "da"),
    GERMAN("german", "de"),
    RUSSIAN("russian", "ru"),
    FRENCH("french", "fr"),
    FILIPINO("filipino", "tl"),
    FINNISH("finnish", "fi"),
    FRISIAN("frisian", "fy"),
    KHMER("khmer", "km"),
    GEORGIAN("georgian", "ka"),
    GUJARATI("gujarati", "gu"),
    KAZAKH("kazakh", "kk"),
    HAITIAN_CREOLE("haitianCreole", "ht"),
    KOREAN("korean", "ko"),
    HAUSA("hausa", "ha"),
    DUTCH("dutch", "nl"),
    KYRGYZ("kyrgyz", "ky"),
    GALICIAN("galician", "gl"),
    CATALAN("catalan", "ca"),
    CZECH("czech", "cs"),
    KANNADA("kannada", "kn"),
    CORSICAN("corsican", "co"),
    CROATIAN("croatian", "hr"),
    KURDISH("kurdish", "ku"),
    LATIN("latin", "la"),
    LATVIAN("latvian", "lv"),
    LAO("lao", "lo"),
    LITHUANIAN("lithuanian", "lt"),
    LUXEMBOURGISH("luxembourgish", "lb"),
    ROMANIAN("romanian", "ro"),
    MALAGASY("malagasy", "mg"),
    MALTESE("maltese", "mt"),
    MARATHI("marathi", "mr"),
    MALAYALAM("malayalam", "ml"),
    MALAY("malay", "ms"),
    MACEDONIAN("macedonian", "mk"),
    MAORI("maori", "mi"),
    MONGOLIAN("mongolian", "mn"),
    BENGALI("bengali", "bn"),
    MYANMAR("myanmar", "my"),
    HMONG("hmong", "hmn"),
    XHOSA("xhosa", "xh"),
    ZULU("zulu", "zu"),
    NEPALI("nepali", "ne"),
    NORWEGIAN("norwegian", "no"),
    PUNJABI("punjabi", "pa"),
    PORTUGUESE("portuguese", "pt"),
    PASHTO("pashto", "ps"),
    CHICHEWA("chichewa", "ny"),
    JAPANESE("japanese", "ja"),
    SWEDISH("swedish", "sv"),
    SAMOAN("samoan", "sm"),
    SERBIAN("serbian", "sr"),
    SESOTHO("sesotho", "st"),
    SINHALA("sinhala", "si"),
    ESPERANTO("esperanto", "eo"),
    SLOVAK("slovak", "sk"),
    SLOVENIAN("slovenian", "sl"),
    SWAHILI("swahili", "sw"),
    SCOTS_GAELIC("scotsGaelic", "gd"),
    CEBUANO("cebuano", "ceb"),
    SOMALI("somali", "so"),
    TAJIK("tajik", "tg"),
    TELUGU("telugu", "te"),
    TAMIL("tamil", "ta"),
    THAI("thai", "th"),
    TURKISH("turkish", "tr"),
    WELSH("welsh", "cy"),
    URDU("urdu", "ur"),
    UKRAINIAN("ukrainian", "uk"),
    UZBEK("uzbek", "uz"),
    SPANISH("spanish", "es"),
    HEBREW("hebrew", "iw"),
    GREEK("greek", "el"),
    HAWAIIAN("hawaiian", "haw"),
    SINDHI("sindhi", "sd"),
    HUNGARIAN("hungarian", "hu"),
    SHONA("shona", "sn"),
    ARMENIAN("armenian", "hy"),
    IGBO("igbo", "ig"),
    ITALIAN("italian", "it"),
    YIDDISH("yiddish", "yi"),
    HINDI("hindi", "hi"),
    SUNDANESE("sundanese", "su"),
    INDONESIAN("indonesian", "id"),
    JAVANESE("javanese", "jw"),
    YORUBA("yoruba", "yo"),
    VIETNAMESE("vietnamese", "vi");

    val langName: String = LanguageBundle.getMessage(langNameKey)

    companion object {
        private val cachedValues: List<Lang> by lazy { values().sortedBy { it.langName } }

        val default: Lang = when (Locale.getDefault().language) {
            Locale.CHINESE.language -> when (Locale.getDefault().country) {
                "HK", "TW" -> CHINESE_TRADITIONAL
                else -> CHINESE
            }

            else -> {
                val language = Locale.getDefault().language
                values().find { it.code.equals(language, ignoreCase = true) } ?: ENGLISH
            }
        }

        fun sortedValues(): List<Lang> = when (Locale.getDefault()) {
            Locale.CHINESE,
            Locale.CHINA -> values().asList()
            else -> cachedValues
        }

        fun valueOfCode(code: String): Lang = when (code) {
            "zh-CHS" -> CHINESE
            else -> values().find { it.code.equals(code, ignoreCase = true) }
                    ?: throw IllegalArgumentException("Unknown language code:$code")
        }
    }
}
