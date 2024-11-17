package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.TranslationDynamicBundle
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import org.jetbrains.annotations.PropertyKey
import java.util.*

private const val LANGUAGE_BUNDLE = "messages.LanguageBundle"

private object LanguageBundle : TranslationDynamicBundle(LANGUAGE_BUNDLE)


@Tag("language-pair")
data class LanguagePair(
    @Attribute var source: Lang = Lang.AUTO,
    @Attribute var target: Lang = Lang.AUTO
)


/**
 * 语言枚举
 *
 * @property languageName 语言英文名称
 * @property code 语言代码
 */
@Suppress("SpellCheckingInspection", "unused")
enum class Lang(
    @Suppress("MemberVisibilityCanBePrivate")
    val languageName: String,
    @PropertyKey(resourceBundle = LANGUAGE_BUNDLE)
    localeNameKey: String,
    val code: String
) {

    /** 自动检测 */
    AUTO("Auto", "auto", "auto"),

    /** 未知语言 */
    UNKNOWN("Unknown", "unknown", "_unknown_"),

    // region ================ LANGUAGES ================

    /** 布尔语(南非荷兰语) */
    AFRIKAANS("Afrikaans", "afrikaans", "af"),

    /** 阿尔巴尼亚语 */
    ALBANIAN("Albanian", "albanian", "sq"),

    /** 阿姆哈拉语 */
    AMHARIC("Amharic", "amharic", "am"),

    /** 阿拉伯语 */
    ARABIC("Arabic", "arabic", "ar"),

    /** 亚美尼亚语 */
    ARMENIAN("Armenian", "armenian", "hy"),

    /** 阿萨姆语 */
    ASSAMESE("Assamese", "assamese", "as"),

    /** 阿塞拜疆语 */
    AZERBAIJANI("Azerbaijani", "azerbaijani", "az"),

    /** 巴什基尔语 */
    BASHKIR("Bashkir", "bashkir", "ba"),

    /** 巴斯克语 */
    BASQUE("Basque", "basque", "eu"),

    /** 白俄罗斯语 */
    BELARUSIAN("Belarusian", "belarusian", "be"),

    /** 孟加拉语 */
    BENGALI("Bengali", "bengali", "bn"),

    /** 波斯尼亚语 */
    BOSNIAN("Bosnian", "bosnian", "bs"),

    /** 保加利亚语 */
    BULGARIAN("Bulgarian", "bulgarian", "bg"),

    /** 加泰罗尼亚语 */
    CATALAN("Catalan", "catalan", "ca"),

    /** 宿务语 */
    CEBUANO("Cebuano", "cebuano", "ceb"),

    /** 齐切瓦语 */
    CHICHEWA("Chichewa", "chichewa", "ny"),

    /** 中文 */
    CHINESE("Chinese", "chinese", "zh"),

    /** 粤语 */
    CHINESE_CANTONESE("Chinese (Cantonese)", "chinese.cantonese", "zh-CANTONESE"),

    /** 文言文 */
    CHINESE_CLASSICAL("Chinese (Classical)", "chinese.classical", "zh-CLASSICAL"),

    /** 中文(简体) */
    CHINESE_SIMPLIFIED("Chinese (Simplified)", "chinese.simplified", "zh-CN"),

    /** 中文(繁体) */
    CHINESE_TRADITIONAL("Chinese (Traditional)", "chinese.traditional", "zh-TW"),

    /** 科西嘉语 */
    CORSICAN("Corsican", "corsican", "co"),

    /** 克罗地亚语 */
    CROATIAN("Croatian", "croatian", "hr"),

    /** 捷克语 */
    CZECH("Czech", "czech", "cs"),

    /** 丹麦语 */
    DANISH("Danish", "danish", "da"),

    /** 达里语 */
    DARI("Dari", "dari", "prs"),

    /** 迪维希语 */
    DIVEHI("Divehi", "divehi", "dv"),

    /** 荷兰语 */
    DUTCH("Dutch", "dutch", "nl"),

    /** 英语 */
    ENGLISH("English", "english", "en"),

    /** 英语(美国) */
    ENGLISH_AMERICAN("English (American)", "english.american", "en-US"),

    /** 英语(英国) */
    ENGLISH_BRITISH("English (British)", "english.british", "en-GB"),

    /** 世界语 */
    ESPERANTO("Esperanto", "esperanto", "eo"),

    /** 爱沙尼亚语 */
    ESTONIAN("Estonian", "estonian", "et"),

    /** 法罗语 */
    FAROESE("Faroese", "faroese", "fo"),

    /** 斐济语 */
    FIJIAN("Fijian", "fijian", "fj"),

    /** 菲律宾语 */
    FILIPINO("Filipino", "filipino", "tl"),

    /** 芬兰语 */
    FINNISH("Finnish", "finnish", "fi"),

    /** 法语 */
    FRENCH("French", "french", "fr"),

    /** 法语 (加拿大) */
    FRENCH_CANADA("French (Canada)", "french.canada", "fr-CA"),

    /** 弗里西语 */
    FRISIAN("Frisian", "frisian", "fy"),

    /** 加利西亚语 */
    GALICIAN("Galician", "galician", "gl"),

    /** 格鲁吉亚语 */
    GEORGIAN("Georgian", "georgian", "ka"),

    /** 德语 */
    GERMAN("German", "german", "de"),

    /** 希腊语 */
    GREEK("Greek", "greek", "el"),

    /** 古吉拉特语 */
    GUJARATI("Gujarati", "gujarati", "gu"),

    /** 海地克里奥尔语 */
    HAITIAN_CREOLE("Haitian Creole", "haitianCreole", "ht"),

    /** 豪萨语 */
    HAUSA("Hausa", "hausa", "ha"),

    /** 夏威夷语 */
    HAWAIIAN("Hawaiian", "hawaiian", "haw"),

    /** 希伯来语 */
    HEBREW("Hebrew", "hebrew", "iw"),

    /** 印地语 */
    HINDI("Hindi", "hindi", "hi"),

    /** 苗语 */
    HMONG("Hmong", "hmong", "hmn"),

    /** 匈牙利语 */
    HUNGARIAN("Hungarian", "hungarian", "hu"),

    /** 冰岛语 */
    ICELANDIC("Icelandic", "icelandic", "is"),

    /** 伊博语 */
    IGBO("Igbo", "igbo", "ig"),

    /** 印尼语 */
    INDONESIAN("Indonesian", "indonesian", "id"),

    /** 爱尔兰语 */
    IRISH("Irish", "irish", "ga"),

    /** 意大利语 */
    ITALIAN("Italian", "italian", "it"),

    /** 日语 */
    JAPANESE("Japanese", "japanese", "ja"),

    /** 印尼爪哇语 */
    JAVANESE("Javanese", "javanese", "jw"),

    /** 卡纳达语 */
    KANNADA("Kannada", "kannada", "kn"),

    /** 哈萨克语 */
    KAZAKH("Kazakh", "kazakh", "kk"),

    /** 高棉语 */
    KHMER("Khmer", "khmer", "km"),

    /** 卢旺达语 */
    KINYARWANDA("Kinyarwanda", "kinyarwanda", "rw"),

    /** 韩语 */
    KOREAN("Korean", "korean", "ko"),

    /** 库尔德语 */
    KURDISH("Kurdish", "kurdish", "ku"),

    /** 库尔德语（库尔曼吉语） */
    KURDISH_KURMANJI("Kurdish (Kurmanji)", "kurdish.kurmanji", "ku"),

    /** 库尔德语（索拉尼） */
    KURDISH_SORANI("Kurdish (Sorani)", "kurdish.sorani", "ckb"),

    /** 吉尔吉斯语 */
    KYRGYZ("Kyrgyz", "kyrgyz", "ky"),

    /** 老挝语 */
    LAO("Lao", "lao", "lo"),

    /** 拉丁语 */
    LATIN("Latin", "latin", "la"),

    /** 拉脱维亚语 */
    LATVIAN("Latvian", "latvian", "lv"),

    /** 立陶宛语 */
    LITHUANIAN("Lithuanian", "lithuanian", "lt"),

    /** 卢森堡语 */
    LUXEMBOURGISH("Luxembourgish", "luxembourgish", "lb"),

    /** 马其顿语 */
    MACEDONIAN("Macedonian", "macedonian", "mk"),

    /** 马尔加什语 */
    MALAGASY("Malagasy", "malagasy", "mg"),

    /** 马来语 */
    MALAY("Malay", "malay", "ms"),

    /** 马拉雅拉姆语 */
    MALAYALAM("Malayalam", "malayalam", "ml"),

    /** 马耳他语 */
    MALTESE("Maltese", "maltese", "mt"),

    /** 毛利语 */
    MAORI("Maori", "maori", "mi"),

    /** 马拉地语 */
    MARATHI("Marathi", "marathi", "mr"),

    /** 蒙古语 */
    MONGOLIAN("Mongolian", "mongolian", "mn"),

    /** 缅甸语 */
    MYANMAR("Myanmar", "myanmar", "my"),

    /** 尼泊尔语 */
    NEPALI("Nepali", "nepali", "ne"),

    /** 挪威语 */
    NORWEGIAN("Norwegian", "norwegian", "no"),

    /** 奥利亚语 */
    ORIYA("Oriya", "oriya", "or"),

    /** 普什图语 */
    PASHTO("Pashto", "pashto", "ps"),

    /** 波斯语 */
    PERSIAN("Persian", "persian", "fa"),

    /** 波兰语 */
    POLISH("Polish", "polish", "pl"),

    /** 葡萄牙语 */
    PORTUGUESE("Portuguese", "portuguese", "pt"),

    /** 葡萄牙语(巴西) */
    PORTUGUESE_BRAZILIAN("Portuguese (Brazilian)", "portuguese.brazilian", "pt-BR"),

    /** 葡萄牙语(葡萄牙) */
    PORTUGUESE_PORTUGUESE("Portuguese (Portugal)", "portuguese.portugal", "pt-PT"),

    /** 旁遮普语 */
    PUNJABI("Punjabi", "punjabi", "pa"),

    /** 罗马尼亚语 */
    ROMANIAN("Romanian", "romanian", "ro"),

    /** 俄语 */
    RUSSIAN("Russian", "russian", "ru"),

    /** 萨摩亚语 */
    SAMOAN("Samoan", "samoan", "sm"),

    /** 苏格兰盖尔语 */
    SCOTS_GAELIC("ScotsGaelic", "scotsGaelic", "gd"),

    /** 塞尔维亚语 */
    SERBIAN("Serbian", "serbian", "sr"),

    /** 塞尔维亚语 (西里尔文) */
    SERBIAN_CYRILLIC("Serbian (cyrillic)", "serbian.cyrillic", "sr-Cyrl"),

    /** 塞尔维亚语 (拉丁文) */
    SERBIAN_LATIN("Serbian (Latin)", "serbian.latin", "sr-Latn"),

    /** 塞索托语 */
    SESOTHO("Sesotho", "sesotho", "st"),

    /** 修纳语 */
    SHONA("Shona", "shona", "sn"),

    /** 信德语 */
    SINDHI("Sindhi", "sindhi", "sd"),

    /** 僧伽罗语 */
    SINHALA("Sinhala", "sinhala", "si"),

    /** 斯洛伐克语 */
    SLOVAK("Slovak", "slovak", "sk"),

    /** 斯洛文尼亚语 */
    SLOVENIAN("Slovenian", "slovenian", "sl"),

    /** 索马里语 */
    SOMALI("Somali", "somali", "so"),

    /** 西班牙语 */
    SPANISH("Spanish", "spanish", "es"),

    /** 印尼巽他语 */
    SUNDANESE("Sundanese", "sundanese", "su"),

    /** 斯瓦希里语 */
    SWAHILI("Swahili", "swahili", "sw"),

    /** 瑞典语 */
    SWEDISH("Swedish", "swedish", "sv"),

    /** 塔希提语 */
    TAHITIAN("Tahitian", "tahitian", "ty"),

    /** 塔吉克语 */
    TAJIK("Tajik", "tajik", "tg"),

    /** 泰米尔语 */
    TAMIL("Tamil", "tamil", "ta"),

    /** 鞑靼语 */
    TATAR("Tatar", "tatar", "tt"),

    /** 泰卢固语 */
    TELUGU("Telugu", "telugu", "te"),

    /** 泰语 */
    THAI("Thai", "thai", "th"),

    /** 藏语 */
    TIBETAN("Tibetan", "tibetan", "bo"),

    /** 提格利尼亚语 */
    TIGRINYA("Tigrinya", "tigrinya", "ti"),

    /** 汤加语 */
    TONGAN("Tongan", "tongan", "to"),

    /** 土耳其语 */
    TURKISH("Turkish", "turkish", "tr"),

    /** 土库曼语 */
    TURKMEN("Turkmen", "turkmen", "tk"),

    /** 乌克兰语 */
    UKRAINIAN("Ukrainian", "ukrainian", "uk"),

    /** 上索布语 */
    UPPER_SORBIAN("Upper Sorbian", "upperSorbian", "hsb"),

    /** 乌尔都语 */
    URDU("Urdu", "urdu", "ur"),

    /** 维吾尔语 */
    UYGHUR("Uyghur", "uyghur", "ug"),

    /** 乌兹别克语(乌孜别克语) */
    UZBEK("Uzbek", "uzbek", "uz"),

    /** 越南语 */
    VIETNAMESE("Vietnamese", "vietnamese", "vi"),

    /** 威尔士语 */
    WELSH("Welsh", "welsh", "cy"),

    /** 南非科萨语 */
    XHOSA("Xhosa", "xhosa", "xh"),

    /** 意第绪语 */
    YIDDISH("Yiddish", "yiddish", "yi"),

    /** 约鲁巴语 */
    YORUBA("Yoruba", "yoruba", "yo"),

    /** 尤卡特克玛雅语 */
    YUCATEC_MAYA("Yucatec Maya", "yucatecMaya", "yua"),

    /** 南非祖鲁语 */
    ZULU("Zulu", "zulu", "zu"),

    ; // endregion


    /** Localized name based on system locale */
    val localeName: String by lazy { LanguageBundle.getMessage(localeNameKey) }

    /** Localized name based on IDE locale */
    val adaptiveLocalName: String by lazy { LanguageBundle.getAdaptedMessage(localeNameKey) }

    companion object {
        private val mapping: Map<String, Lang> by lazy { values().associateBy { it.code } }

        /**
         * Sorted languages (excluding [UNKNOWN]) by their locale names.
         */
        val sortedLanguages: List<Lang> by lazy {
            values()
                .asSequence()
                .filter { it != UNKNOWN }
                .sortedBy { if (it == AUTO) "" else it.localeName }
                .toList()
                .let { Collections.unmodifiableList(it) }
        }

        /**
         * Returns the default language.
         */
        val default: Lang
            get() {
                val dynamicBundleLanguage = (TranslationDynamicBundle.dynamicLocale ?: Locale.ENGLISH).language
                val localeLanguage =
                    if (dynamicBundleLanguage != Locale.ENGLISH.language) dynamicBundleLanguage
                    else Locale.getDefault().language

                return when (localeLanguage) {
                    Locale.CHINESE.language -> when (Locale.getDefault().country) {
                        "HK", "TW" -> CHINESE_TRADITIONAL
                        else -> CHINESE_SIMPLIFIED
                    }

                    else -> values().find { Locale(it.code).language.equals(localeLanguage, ignoreCase = true) }
                        ?: ENGLISH
                }
            }

        /**
         * Returns the [language][Lang] corresponding to the given [code].
         */
        operator fun get(code: String): Lang = mapping[code] ?: UNKNOWN

        /**
         * Returns `true` if this language is [AUTO].
         */
        fun Lang.isAuto(): Boolean = this == AUTO

        /**
         * Returns `true` if this language is [UNKNOWN].
         */
        fun Lang.isUnknown(): Boolean = this == UNKNOWN

        /**
         * Tests if this language is explicit (not [AUTO] and not [UNKNOWN]).
         */
        fun Lang.isExplicit(): Boolean = this != AUTO && this != UNKNOWN

        /**
         * Try to convert to an explicit language: Returns [UNKNOWN] (even though [UNKNOWN]
         * is not an explicit language) if it is [AUTO], otherwise return itself.
         */
        fun Lang.toExplicit(): Lang = when (this) {
            AUTO -> UNKNOWN
            else -> this
        }
    }
}
