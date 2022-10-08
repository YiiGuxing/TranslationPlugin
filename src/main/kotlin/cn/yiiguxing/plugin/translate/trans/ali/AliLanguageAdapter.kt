package cn.yiiguxing.plugin.translate.trans.ali

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

object AliLanguageAdapter : BaseLanguageAdapter() {

    override val supportedSourceLanguages: List<Lang> = listOf(
        Lang.AUTO,
        Lang.CHINESE,
        Lang.CHINESE_TRADITIONAL,
        Lang.ENGLISH,
        Lang.JAPANESE,
        Lang.KOREAN,
        Lang.FRENCH,
        Lang.SPANISH,
        Lang.ITALIAN,
        Lang.GERMAN,
        Lang.TURKISH,
        Lang.RUSSIAN,
        Lang.PORTUGUESE,
        Lang.VIETNAMESE,
        Lang.INDONESIAN,
        Lang.THAI,
        Lang.MALAY,
        Lang.ARABIC,
        Lang.HINDI
    )

    override val supportedTargetLanguages: List<Lang> = listOf(
        Lang.CHINESE,
        Lang.ENGLISH,
        Lang.JAPANESE,
        Lang.KOREAN,
        Lang.FRENCH,
        Lang.SPANISH,
        Lang.ITALIAN,
        Lang.GERMAN,
        Lang.TURKISH,
        Lang.RUSSIAN,
        Lang.PORTUGUESE,
        Lang.VIETNAMESE,
        Lang.INDONESIAN,
        Lang.THAI,
        Lang.MALAY
    )

    override fun getAdaptedLanguages(): Map<String, Lang> = mapOf(
        "zh" to Lang.CHINESE,
        "zh-tw" to Lang.CHINESE_TRADITIONAL,
        "yue" to Lang.CHINESE_CANTONESE,
        "fil" to Lang.FILIPINO,
        "hbs" to Lang.CROATIAN,
        "he" to Lang.HEBREW,
    )

}

val Lang.aliLanguageCode: String
    get() = AliLanguageAdapter.getLanguageCode(this)

@Suppress("unused")
fun Lang.Companion.fromAliLanguageCode(code: String): Lang {
    return AliLanguageAdapter.getLanguage(code)
}