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
 * 语言
 */
@Suppress("SpellCheckingInspection", "unused")
enum class Lang(
    @PropertyKey(resourceBundle = LANGUAGE_BUNDLE)
    namePropertyKey: String,
    val code: String
) {

    /** 自动检测 */
    AUTO("auto", "auto"),

    /** 未知语言 */
    UNKNOWN("unknown", "_unknown_"),

    // region ================ LANGUAGES ================

    /** 布尔语(南非荷兰语) */
    AFRIKAANS("afrikaans", "af"),

    /** 阿尔巴尼亚语 */
    ALBANIAN("albanian", "sq"),

    /** 阿姆哈拉语 */
    AMHARIC("amharic", "am"),

    /** 阿拉伯语 */
    ARABIC("arabic", "ar"),

    /** 亚美尼亚语 */
    ARMENIAN("armenian", "hy"),

    /** 阿萨姆语 */
    ASSAMESE("assamese", "as"),

    /** 阿塞拜疆语 */
    AZERBAIJANI("azerbaijani", "az"),

    /** 巴什基尔语 */
    BASHKIR("bashkir", "ba"),

    /** 巴斯克语 */
    BASQUE("basque", "eu"),

    /** 白俄罗斯语 */
    BELARUSIAN("belarusian", "be"),

    /** 孟加拉语 */
    BENGALI("bengali", "bn"),

    /** 波斯尼亚语 */
    BOSNIAN("bosnian", "bs"),

    /** 保加利亚语 */
    BULGARIAN("bulgarian", "bg"),

    /** 加泰罗尼亚语 */
    CATALAN("catalan", "ca"),

    /** 宿务语 */
    CEBUANO("cebuano", "ceb"),

    /** 齐切瓦语 */
    CHICHEWA("chichewa", "ny"),

    /** 中文 */
    CHINESE("chinese", "zh-CN"),

    /** 粤语 */
    CHINESE_CANTONESE("chinese.cantonese", "zh-CANTONESE"),

    /** 文言文 */
    CHINESE_CLASSICAL("chinese.classical", "zh-CLASSICAL"),

    /** 中文(繁体) */
    CHINESE_TRADITIONAL("chinese.traditional", "zh-TW"),

    /** 科西嘉语 */
    CORSICAN("corsican", "co"),

    /** 克罗地亚语 */
    CROATIAN("croatian", "hr"),

    /** 捷克语 */
    CZECH("czech", "cs"),

    /** 丹麦语 */
    DANISH("danish", "da"),

    /** 达里语 */
    DARI("dari", "prs"),

    /** 迪维希语 */
    DIVEHI("divehi", "dv"),

    /** 荷兰语 */
    DUTCH("dutch", "nl"),

    /** 英语 */
    ENGLISH("english", "en"),

    /** 英语(美国) */
    ENGLISH_AMERICAN("english.american", "en-US"),

    /** 英语(英国) */
    ENGLISH_BRITISH("english.british", "en-GB"),

    /** 世界语 */
    ESPERANTO("esperanto", "eo"),

    /** 爱沙尼亚语 */
    ESTONIAN("estonian", "et"),

    /** 法罗语 */
    FAROESE("faroese", "fo"),

    /** 斐济语 */
    FIJIAN("fijian", "fj"),

    /** 菲律宾语 */
    FILIPINO("filipino", "tl"),

    /** 芬兰语 */
    FINNISH("finnish", "fi"),

    /** 法语 */
    FRENCH("french", "fr"),

    /** 法语 (加拿大) */
    FRENCH_CANADA("french.canada", "fr-CA"),

    /** 弗里西语 */
    FRISIAN("frisian", "fy"),

    /** 加利西亚语 */
    GALICIAN("galician", "gl"),

    /** 格鲁吉亚语 */
    GEORGIAN("georgian", "ka"),

    /** 德语 */
    GERMAN("german", "de"),

    /** 希腊语 */
    GREEK("greek", "el"),

    /** 古吉拉特语 */
    GUJARATI("gujarati", "gu"),

    /** 海地克里奥尔语 */
    HAITIAN_CREOLE("haitianCreole", "ht"),

    /** 豪萨语 */
    HAUSA("hausa", "ha"),

    /** 夏威夷语 */
    HAWAIIAN("hawaiian", "haw"),

    /** 希伯来语 */
    HEBREW("hebrew", "iw"),

    /** 印地语 */
    HINDI("hindi", "hi"),

    /** 苗语 */
    HMONG("hmong", "hmn"),

    /** 匈牙利语 */
    HUNGARIAN("hungarian", "hu"),

    /** 冰岛语 */
    ICELANDIC("icelandic", "is"),

    /** 伊博语 */
    IGBO("igbo", "ig"),

    /** 印尼语 */
    INDONESIAN("indonesian", "id"),

    /** 爱尔兰语 */
    IRISH("irish", "ga"),

    /** 意大利语 */
    ITALIAN("italian", "it"),

    /** 日语 */
    JAPANESE("japanese", "ja"),

    /** 印尼爪哇语 */
    JAVANESE("javanese", "jw"),

    /** 卡纳达语 */
    KANNADA("kannada", "kn"),

    /** 哈萨克语 */
    KAZAKH("kazakh", "kk"),

    /** 高棉语 */
    KHMER("khmer", "km"),

    /** 卢旺达语 */
    KINYARWANDA("kinyarwanda", "rw"),

    /** 韩语 */
    KOREAN("korean", "ko"),

    /** 库尔德语 */
    KURDISH("kurdish", "ku"),

    /** 吉尔吉斯语 */
    KYRGYZ("kyrgyz", "ky"),

    /** 老挝语 */
    LAO("lao", "lo"),

    /** 拉丁语 */
    LATIN("latin", "la"),

    /** 拉脱维亚语 */
    LATVIAN("latvian", "lv"),

    /** 立陶宛语 */
    LITHUANIAN("lithuanian", "lt"),

    /** 卢森堡语 */
    LUXEMBOURGISH("luxembourgish", "lb"),

    /** 马其顿语 */
    MACEDONIAN("macedonian", "mk"),

    /** 马尔加什语 */
    MALAGASY("malagasy", "mg"),

    /** 马来语 */
    MALAY("malay", "ms"),

    /** 马拉雅拉姆语 */
    MALAYALAM("malayalam", "ml"),

    /** 马耳他语 */
    MALTESE("maltese", "mt"),

    /** 毛利语 */
    MAORI("maori", "mi"),

    /** 马拉地语 */
    MARATHI("marathi", "mr"),

    /** 蒙古语 */
    MONGOLIAN("mongolian", "mn"),

    /** 缅甸语 */
    MYANMAR("myanmar", "my"),

    /** 尼泊尔语 */
    NEPALI("nepali", "ne"),

    /** 挪威语 */
    NORWEGIAN("norwegian", "no"),

    /** 奥利亚语 */
    ORIYA("oriya", "or"),

    /** 普什图语 */
    PASHTO("pashto", "ps"),

    /** 波斯语 */
    PERSIAN("persian", "fa"),

    /** 波兰语 */
    POLISH("polish", "pl"),

    /** 葡萄牙语 */
    PORTUGUESE("portuguese", "pt"),

    /** 葡萄牙语(巴西) */
    PORTUGUESE_BRAZILIAN("portuguese.brazilian", "pt-BR"),

    /** 葡萄牙语(葡萄牙) */
    PORTUGUESE_PORTUGUESE("portuguese.portugal", "pt-PT"),

    /** 旁遮普语 */
    PUNJABI("punjabi", "pa"),

    /** 罗马尼亚语 */
    ROMANIAN("romanian", "ro"),

    /** 俄语 */
    RUSSIAN("russian", "ru"),

    /** 萨摩亚语 */
    SAMOAN("samoan", "sm"),

    /** 苏格兰盖尔语 */
    SCOTS_GAELIC("scotsGaelic", "gd"),

    /** 塞尔维亚语 */
    SERBIAN("serbian", "sr"),

    /** 塞尔维亚语 (西里尔文) */
    SERBIAN_CYRILLIC("serbian.cyrillic", "sr-Cyrl"),

    /** 塞尔维亚语 (拉丁文) */
    SERBIAN_LATIN("serbian.latin", "sr-Latn"),

    /** 塞索托语 */
    SESOTHO("sesotho", "st"),

    /** 修纳语 */
    SHONA("shona", "sn"),

    /** 信德语 */
    SINDHI("sindhi", "sd"),

    /** 僧伽罗语 */
    SINHALA("sinhala", "si"),

    /** 斯洛伐克语 */
    SLOVAK("slovak", "sk"),

    /** 斯洛文尼亚语 */
    SLOVENIAN("slovenian", "sl"),

    /** 索马里语 */
    SOMALI("somali", "so"),

    /** 西班牙语 */
    SPANISH("spanish", "es"),

    /** 印尼巽他语 */
    SUNDANESE("sundanese", "su"),

    /** 斯瓦希里语 */
    SWAHILI("swahili", "sw"),

    /** 瑞典语 */
    SWEDISH("swedish", "sv"),

    /** 塔希提语 */
    TAHITIAN("tahitian", "ty"),

    /** 塔吉克语 */
    TAJIK("tajik", "tg"),

    /** 泰米尔语 */
    TAMIL("tamil", "ta"),

    /** 鞑靼语 */
    TATAR("tatar", "tt"),

    /** 泰卢固语 */
    TELUGU("telugu", "te"),

    /** 泰语 */
    THAI("thai", "th"),

    /** 藏语 */
    TIBETAN("tibetan", "bo"),

    /** 提格利尼亚语 */
    TIGRINYA("tigrinya", "ti"),

    /** 汤加语 */
    TONGAN("tongan", "to"),

    /** 土耳其语 */
    TURKISH("turkish", "tr"),

    /** 土库曼语 */
    TURKMEN("turkmen", "tk"),

    /** 乌克兰语 */
    UKRAINIAN("ukrainian", "uk"),

    /** 上索布语 */
    UPPER_SORBIAN("upperSorbian", "hsb"),

    /** 乌尔都语 */
    URDU("urdu", "ur"),

    /** 维吾尔语 */
    UYGHUR("uyghur", "ug"),

    /** 乌兹别克语(乌孜别克语) */
    UZBEK("uzbek", "uz"),

    /** 越南语 */
    VIETNAMESE("vietnamese", "vi"),

    /** 威尔士语 */
    WELSH("welsh", "cy"),

    /** 南非科萨语 */
    XHOSA("xhosa", "xh"),

    /** 意第绪语 */
    YIDDISH("yiddish", "yi"),

    /** 约鲁巴语 */
    YORUBA("yoruba", "yo"),

    /** 尤卡特克玛雅语 */
    YUCATEC_MAYA("yucatecMaya", "yua"),

    /** 南非祖鲁语 */
    ZULU("zulu", "zu"),

    ; // endregion


    val langName: String by lazy { LanguageBundle.getMessage(namePropertyKey) }

    companion object {
        private val mapping: Map<String, Lang> by lazy { values().asSequence().map { it.code to it }.toMap() }

        val sortedLanguages: List<Lang> by lazy {
            values()
                .asSequence()
                .filter { it != UNKNOWN }
                .sortedBy { if (it == AUTO) "" else it.langName }
                .toList()
                .let { Collections.unmodifiableList(it) }
        }

        val default: Lang
            get() {
                val dynamicBundleLanguage = (TranslationDynamicBundle.dynamicLocale ?: Locale.ENGLISH).language
                val localeLanguage =
                    if (dynamicBundleLanguage != Locale.ENGLISH.language) dynamicBundleLanguage
                    else Locale.getDefault().language

                return when (localeLanguage) {
                    Locale.CHINESE.language -> when (Locale.getDefault().country) {
                        "HK", "TW" -> CHINESE_TRADITIONAL
                        else -> CHINESE
                    }

                    else -> values().find { Locale(it.code).language.equals(localeLanguage, ignoreCase = true) }
                        ?: ENGLISH
                }
            }

        /**
         * Returns the [language][Lang] corresponding to the given [code].
         */
        operator fun get(code: String): Lang = mapping[code] ?: UNKNOWN

    }
}
