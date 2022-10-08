package cn.yiiguxing.plugin.translate.trans.youdao

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

object YoudaoLanguageAdapter : BaseLanguageAdapter() {

    private val SUPPORTED_LANGUAGES: List<Lang> = (Lang.sortedValues() - listOf(
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

val Lang.youdaoLanguageCode: String
    get() = YoudaoLanguageAdapter.getLanguageCode(this)

fun Lang.Companion.fromYoudaoLanguageCode(code: String): Lang {
    return YoudaoLanguageAdapter.getLanguage(code)
}