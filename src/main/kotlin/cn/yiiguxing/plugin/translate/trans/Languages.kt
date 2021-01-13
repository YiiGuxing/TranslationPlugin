package cn.yiiguxing.plugin.translate.trans

import com.intellij.AbstractBundle
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.*

private const val LANGUAGE_BUNDLE = "messages.LanguageBundle"

private object LanguageBundle : AbstractBundle(LANGUAGE_BUNDLE)


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
    langNameKey: String,
    val code: String,
    youdaoCode: String? = null,
    baiduCode: String? = null
) {

    /** 自动检测 */
    AUTO("auto", "auto"),

    /** 中文 */
    CHINESE("chinese", "zh-CN", "zh-CHS", "zh"),

    /** 英语 */
    ENGLISH("english", "en"),

    /** 中文(繁体) */
    CHINESE_TRADITIONAL("chinese.traditional", "zh-TW", baiduCode = "cht"),

    /** 文言文 */
    CHINESE_CLASSICAL("chinese.classical", "zh-CLASSICAL", baiduCode = "wyw"),

    /** 粤语 */
    CHINESE_CANTONESE("chinese.cantonese", "zh-CANTONESE", "yue", baiduCode = "yue"),

    /** 阿尔巴尼亚语 */
    ALBANIAN("albanian", "sq"),

    /** 阿拉伯语 */
    ARABIC("arabic", "ar", baiduCode = "ara"),

    /** 阿姆哈拉语 */
    AMHARIC("amharic", "am"),

    /** 阿塞拜疆语 */
    AZERBAIJANI("azerbaijani", "az"),

    /** 爱尔兰语 */
    IRISH("irish", "ga"),

    /** 爱沙尼亚语 */
    ESTONIAN("estonian", "et", baiduCode = "est"),

    /** 巴斯克语 */
    BASQUE("basque", "eu"),

    /** 白俄罗斯语 */
    BELARUSIAN("belarusian", "be"),

    /** 保加利亚语 */
    BULGARIAN("bulgarian", "bg", baiduCode = "bul"),

    /** 冰岛语 */
    ICELANDIC("icelandic", "is"),

    /** 波兰语 */
    POLISH("polish", "pl"),

    /** 波斯尼亚语 */
    BOSNIAN("bosnian", "bs"),

    /** 波斯语 */
    PERSIAN("persian", "fa"),

    /** 布尔语(南非荷兰语) */
    AFRIKAANS("afrikaans", "af"),

    /** 丹麦语 */
    DANISH("danish", "da", baiduCode = "dan"),

    /** 德语 */
    GERMAN("german", "de"),

    /** 俄语 */
    RUSSIAN("russian", "ru"),

    /** 法语 */
    FRENCH("french", "fr", baiduCode = "fra"),

    /** 菲律宾语 */
    FILIPINO("filipino", "tl"),

    /** 芬兰语 */
    FINNISH("finnish", "fi", baiduCode = "fin"),

    /** 弗里西语 */
    FRISIAN("frisian", "fy"),

    /** 高棉语 */
    KHMER("khmer", "km"),

    /** 格鲁吉亚语 */
    GEORGIAN("georgian", "ka"),

    /** 古吉拉特语 */
    GUJARATI("gujarati", "gu"),

    /** 哈萨克语 */
    KAZAKH("kazakh", "kk"),

    /** 海地克里奥尔语 */
    HAITIAN_CREOLE("haitianCreole", "ht"),

    /** 韩语 */
    KOREAN("korean", "ko", baiduCode = "kor"),

    /** 豪萨语 */
    HAUSA("hausa", "ha"),

    /** 荷兰语 */
    DUTCH("dutch", "nl"),

    /** 吉尔吉斯语 */
    KYRGYZ("kyrgyz", "ky"),

    /** 加利西亚语 */
    GALICIAN("galician", "gl"),

    /** 加泰罗尼亚语 */
    CATALAN("catalan", "ca"),

    /** 捷克语 */
    CZECH("czech", "cs"),

    /** 卡纳达语 */
    KANNADA("kannada", "kn"),

    /** 科西嘉语 */
    CORSICAN("corsican", "co"),

    /** 克罗地亚语 */
    CROATIAN("croatian", "hr"),

    /** 库尔德语 */
    KURDISH("kurdish", "ku"),

    /** 拉丁语 */
    LATIN("latin", "la"),

    /** 拉脱维亚语 */
    LATVIAN("latvian", "lv"),

    /** 老挝语 */
    LAO("lao", "lo"),

    /** 立陶宛语 */
    LITHUANIAN("lithuanian", "lt"),

    /** 卢森堡语 */
    LUXEMBOURGISH("luxembourgish", "lb"),

    /** 罗马尼亚语 */
    ROMANIAN("romanian", "ro", baiduCode = "rom"),

    /** 马尔加什语 */
    MALAGASY("malagasy", "mg"),

    /** 马耳他语 */
    MALTESE("maltese", "mt"),

    /** 马拉地语 */
    MARATHI("marathi", "mr"),

    /** 马拉雅拉姆语 */
    MALAYALAM("malayalam", "ml"),

    /** 马来语 */
    MALAY("malay", "ms"),

    /** 马其顿语 */
    MACEDONIAN("macedonian", "mk"),

    /** 毛利语 */
    MAORI("maori", "mi"),

    /** 蒙古语 */
    MONGOLIAN("mongolian", "mn"),

    /** 孟加拉语 */
    BENGALI("bengali", "bn"),

    /** 缅甸语 */
    MYANMAR("myanmar", "my"),

    /** 苗语 */
    HMONG("hmong", "hmn"),

    /** 南非科萨语 */
    XHOSA("xhosa", "xh"),

    /** 南非祖鲁语 */
    ZULU("zulu", "zu"),

    /** 尼泊尔语 */
    NEPALI("nepali", "ne"),

    /** 挪威语 */
    NORWEGIAN("norwegian", "no"),

    /** 旁遮普语 */
    PUNJABI("punjabi", "pa"),

    /** 葡萄牙语 */
    PORTUGUESE("portuguese", "pt"),

    /** 普什图语 */
    PASHTO("pashto", "ps"),

    /** 齐切瓦语 */
    CHICHEWA("chichewa", "ny"),

    /** 日语 */
    JAPANESE("japanese", "ja", baiduCode = "jp"),

    /** 瑞典语 */
    SWEDISH("swedish", "sv", baiduCode = "swe"),

    /** 萨摩亚语 */
    SAMOAN("samoan", "sm"),

    /** 塞尔维亚语 */
    SERBIAN("serbian", "sr"),

    /** 塞索托语 */
    SESOTHO("sesotho", "st"),

    /** 僧伽罗语 */
    SINHALA("sinhala", "si"),

    /** 世界语 */
    ESPERANTO("esperanto", "eo"),

    /** 斯洛伐克语 */
    SLOVAK("slovak", "sk"),

    /** 斯洛文尼亚语 */
    SLOVENIAN("slovenian", "sl", baiduCode = "slo"),

    /** 斯瓦希里语 */
    SWAHILI("swahili", "sw"),

    /** 苏格兰盖尔语 */
    SCOTS_GAELIC("scotsGaelic", "gd"),

    /** 宿务语 */
    CEBUANO("cebuano", "ceb"),

    /** 索马里语 */
    SOMALI("somali", "so"),

    /** 塔吉克语 */
    TAJIK("tajik", "tg"),

    /** 泰卢固语 */
    TELUGU("telugu", "te"),

    /** 泰米尔语 */
    TAMIL("tamil", "ta"),

    /** 泰语 */
    THAI("thai", "th"),

    /** 土耳其语 */
    TURKISH("turkish", "tr"),

    /** 威尔士语 */
    WELSH("welsh", "cy"),

    /** 乌尔都语 */
    URDU("urdu", "ur"),

    /** 乌克兰语 */
    UKRAINIAN("ukrainian", "uk"),

    /** 乌兹别克语 */
    UZBEK("uzbek", "uz"),

    /** 西班牙语 */
    SPANISH("spanish", "es", baiduCode = "spa"),

    /** 希伯来语 */
    HEBREW("hebrew", "iw", "he"),

    /** 希腊语 */
    GREEK("greek", "el"),

    /** 夏威夷语 */
    HAWAIIAN("hawaiian", "haw"),

    /** 信德语 */
    SINDHI("sindhi", "sd"),

    /** 匈牙利语 */
    HUNGARIAN("hungarian", "hu"),

    /** 修纳语 */
    SHONA("shona", "sn"),

    /** 亚美尼亚语 */
    ARMENIAN("armenian", "hy"),

    /** 伊博语 */
    IGBO("igbo", "ig"),

    /** 意大利语 */
    ITALIAN("italian", "it"),

    /** 意第绪语 */
    YIDDISH("yiddish", "yi"),

    /** 印地语 */
    HINDI("hindi", "hi"),

    /** 印尼巽他语 */
    SUNDANESE("sundanese", "su"),

    /** 印尼语 */
    INDONESIAN("indonesian", "id"),

    /** 印尼爪哇语 */
    JAVANESE("javanese", "jw"),

    /** 约鲁巴语 */
    YORUBA("yoruba", "yo"),

    /** 越南语 */
    VIETNAMESE("vietnamese", "vi", baiduCode = "vie");

    val langName: String = LanguageBundle.getMessage(langNameKey)
    val youdaoCode: String = youdaoCode ?: code
    val baiduCode: String = baiduCode ?: code

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

        fun valueOfCode(code: String): Lang = values()
            .find { it.code.equals(code, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown language code:$code")

        fun valueOfYoudaoCode(code: String): Lang = values()
            .find { it.youdaoCode.equals(code, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown language code:$code")

        fun valueOfBaiduCode(code: String): Lang = values()
            .find { it.baiduCode.equals(code, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown language code:$code")
    }
}
