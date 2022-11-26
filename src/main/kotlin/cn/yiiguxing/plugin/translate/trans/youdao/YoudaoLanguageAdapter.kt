package cn.yiiguxing.plugin.translate.trans.youdao

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

/**
 * Language adapter for Youdao Translator.
 */
object YoudaoLanguageAdapter : BaseLanguageAdapter() {

    private val SUPPORTED_LANGUAGES: List<Lang> = (Lang.sortedLanguages - listOf(
        Lang.CHINESE_TRADITIONAL,
        Lang.CHINESE_CLASSICAL,
        Lang.AFRIKAANS,
        Lang.KYRGYZ,
        Lang.CATALAN,
        Lang.HMONG,
        Lang.SERBIAN,
        Lang.SLOVENIAN
    )).toList()

    override val supportedSourceLanguages: List<Lang> = SUPPORTED_LANGUAGES

    override val supportedTargetLanguages: List<Lang> = SUPPORTED_LANGUAGES

    override fun getAdaptedLanguages(): Map<String, Lang> = mapOf(
        "zh-CHS" to Lang.CHINESE,
        "yue" to Lang.CHINESE_CANTONESE,
        "he" to Lang.HEBREW,
    )

}


/**
 * Language code for Youdao Translator.
 */
val Lang.youdaoLanguageCode: String
    get() = YoudaoLanguageAdapter.getLanguageCode(this)

/**
 * Returns the [language][Lang] for the specified Youdao Translator language [code].
 */
fun Lang.Companion.fromYoudaoLanguageCode(code: String): Lang {
    return YoudaoLanguageAdapter.getLanguage(code)
}