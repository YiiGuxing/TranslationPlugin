package cn.yiiguxing.plugin.translate.trans.youdao

import cn.yiiguxing.plugin.translate.trans.BaseLanguageAdapter
import cn.yiiguxing.plugin.translate.trans.Lang

object YoudaoLanguageAdapter : BaseLanguageAdapter() {

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